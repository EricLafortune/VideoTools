* Video player for the TI-99/4A home computer.
*
* Copyright (c) 2022-2024 Eric Lafortune
*
* This program is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by the Free
* Software Foundation; either version 2 of the License, or (at your option)
* any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
* more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

    copy "include/colors.asm"
    copy "include/cpu.asm"
    copy "include/vdp.asm"
    copy "include/sound.asm"
    copy "include/speech.asm"
    copy "include/cru.asm"

* A single workspace at the end of scratch-pad RAM.
workspace equ scratchpad_end - workspace_size

* Locations of the various graphics tables in VDP memory.
pattern_descriptor_table equ >0000 ; Size >1800.
color_table              equ >2000 ; Size >1800.
screen_image_table       equ >1800 ; Size >0300.
sprite_attribute_table   equ >3800 ; Size >0080.
sprite_descriptor_table  equ >0000 ; Size >0800.

* Start in module ROM/RAM.
    aorg module_memory

* Module header.
    byte >aa                   ; Header.
    byte 1                     ; Version.
    byte 1                     ; Number of programs.
    byte 0                     ; Unused.
    data 0                     ; Power-up list.
    data program_list          ; Program list (shown in reverse order).
    data 0                     ; DSR list.
    data 0                     ; Subprogram list.

* First element of the program list.
program_list
    data !                     ; Next element.
    data play_with_speech      ; Program start.
    stri 'VIDEO WITH SPEECH'   ; Program name.

* Second element of the program list.
!   data 0                     ; Next element.
    data play_without_speech   ; Program start.
    stri 'VIDEO'               ; Program name.
    even

play_without_speech
    limi 0
    lwpi workspace

    clr  r10                   ; Cache a dummy speech address.
    jmp  !

play_with_speech
    limi 0
    lwpi workspace

    .spchwt_in_register r10    ; Cache the speech address.
!   .sound_in_register  r11    ; Cache the sound address.
    .vdpwa_in_register  r13    ; Cache the vdpwa address.
    .vdpwd_in_register  r14    ; Cache the vdpwd address.
    .vdpsta_in_register r15    ; Cache the vdpsta address.

    .vdpwr_mode                     bitmap_mode
    .vdpwr_flags                    vdp16k | display_enable | interrupt_enable
    .vdpwr_pattern_descriptor_table pattern_descriptor_table, bitmap_mode
    .vdpwr_color_table              color_table, bitmap_mode
    .vdpwr_screen_image_table       screen_image_table
    .vdpwr_sprite_attribute_table   sprite_attribute_table
    .vdpwr_sprite_descriptor_table  sprite_descriptor_table
    .vdpwr_background_color         black

* Initialize the pattern descriptor table and screen image table.
    .vdpwa pattern_descriptor_table | vdp_write_bit
    clr  r0
    li   r1, bitmap_pattern_descriptor_table_size + screen_image_table_size
pattern_loop
    .vdpwd r0
    dec  r1
    jne  pattern_loop

* Initialize the color table.
    .vdpwa    color_table | vdp_write_bit
    .li_color r0, white, black
    li        r1, bitmap_color_table_size
color_loop
    .vdpwd r0
    dec  r1
    jne  color_loop

* Initialize the sprite attribute table.
    .vdpwa sprite_attribute_table | vdp_write_bit
    li     r0, sprite_attribute_table_terminator * 256
    .vdpwd r0

* Copy the player code to scratchpad RAM and run it.
    li   r0, code_start
    li   r1, scratchpad
copy_loop
    mov  *r0+, *r1+
    ci   r0, code_end
    jne  copy_loop

    b    @scratchpad

code_start
    xorg scratchpad

* Main loop that draws all frames, in scratchpad RAM for speed.
* Registers:
*   r0:  Current adress to set the module memory bank: >6000, >6002,...,>7ffe.
*   r1:  Data pointer in the current memory bank, starting at >6000.
*   r2:  Command and count.
*   r10: SPCHWT constant.
*   r11: SOUND constant.
*   r12: CRU base.
*   r13: VDPWD constant.
*   r14: VDPWA constant.
*   r15: VDPSTA constant.
movie_loop
    li   r0, module_bank_selection + module_bank_increment ; Set the first animation bank.

* Switch to the current bank and update the number.
bank_loop
    .switch_bank *r0           ; Switch to the current bank.
    inct r0                    ; Increment the bank index.

    li   r1, module_memory     ; Set the first frame.

* Stream a chunk (video, sound, or speech).
frame_loop
    movb *r1+, r2              ; Get the pre-swapped command/count.
    swpb r2
    movb *r1+, r2
    jlt  check_sound_chunk

* Stream a chunk of video data.

; Simple version without loop unrolling.
;                               ; Write the pre-swapped VDP address.
;    movb *r1+, *r_vdpwa        ;: d-
;    nop
;    movb *r1+, *r_vdpwa        ;: d-
;    nop
;
;video_loop                     ; Copy the data to VDP RAM.
;    movb *r1+, *r_vdpwd        ;: d-
;    dec  r2
;    jne  video_loop
;    jmp  frame_loop

; Faster version with loop unrolling.
                               ; Write the pre-swapped VDP address.
    movb *r1+, *r_vdpwa        ;: d-

    mov  r2, r3                ; Inbetween: compute the branch offset into the
    andi r3, >0007             ; unrolled loop.
    sla  r3, 1
    neg  r3

    movb *r1+, *r_vdpwa        ;: d-

    srl  r2, 3                 ; Compute the unrolled loop count.
    inc  r2
    b @unrolled_video_loop_end(r3)

unrolled_video_loop            ; Copy the data to VDP RAM.
    .vdpwd *r1+                ;: d-
    .vdpwd *r1+                ;: d-
    .vdpwd *r1+                ;: d-
    .vdpwd *r1+                ;: d-
    .vdpwd *r1+                ;: d-
    .vdpwd *r1+                ;: d-
    .vdpwd *r1+                ;: d-
    .vdpwd *r1+                ;: d-
unrolled_video_loop_end
    dec  r2
    jne  unrolled_video_loop
    jmp  frame_loop

check_sound_chunk
    ai   r2, >0020             ; Did we get a sound chunk?
    jlt  check_speech_chunk

* Stream a chunk of sound data.
sound_loop                     ; Copy the data to the sound processor.
    .sound *r1+                ;: d-
    dec  r2
    jne  sound_loop
    jmp  frame_loop            ; Continue with the rest of the frame.

check_speech_chunk
    ai   r2, >0010             ; Did we get a speech video?
    jlt  check_vsync_marker

* Stream a chunk of speech data.
speech_loop                    ; Copy the data to the speech synthesizer.
    .spchwt *r1+               ;: d-
    dec  r2
    jne  speech_loop
    jmp  frame_loop            ; Continue with the rest of the frame.

check_vsync_marker
    inc  r2                    ; Did we get a VSYNC marker?
    jne  check_next_bank_marker

* Wait for VSYNC.
    .wait_for_vsync
    jmp  frame_loop            ; Continue with the rest of the frame.

check_next_bank_marker
    inc  r2                    ; Did we get a NEXT_BANK marker?
    jeq  bank_loop             ; Then continue with the next bank.

                               ; Otherwise we got an EOF marker.
* Wait for a key press (in key column 0).
    li   r12, cru_write_keyboard_column
    clr  r0
    ldcr r0, cru_keyboard_column_bit_count
    li   r12, cru_read_keyboard_rows

key_press_loop
    stcr r0, cru_read_keyboard_row_bit_count
    ci   r0, >ff00
    jeq  key_press_loop

    .switch_bank @module_bank_selection ; Reset to the first bank.

    blwp @reset_vector         ; Return to the title screen.

    .ifgt  $, workspace
    .error 'Scratch-pad code block too large'
    .endif

    aorg
code_end

    .ifgt  $, module_memory_end
    .error 'Cartridge code too large'
    .endif

    aorg module_memory_end
    ;copy "../data/video.asm"
    bcopy "../data/video.tms"
