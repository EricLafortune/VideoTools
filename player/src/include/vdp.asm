* Useful assembly definitions for the TI-99/4A home computer.
*
* Copyright (c) 2021-2024 Eric Lafortune
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

******************************************************************************
* Definitions and macros for accessing the Video Display Processor (VDP) and
* VDP RAM.
*
* The VDP is documented in
*     http://www.unige.ch/medecine/nouspikel/ti99/tms9918a.htm
******************************************************************************

* VDP register write addresses (including the register bit >8000).
vdp_r0 equ >8000
vdp_r1 equ >8100
vdp_r2 equ >8200
vdp_r3 equ >8300
vdp_r4 equ >8400
vdp_r5 equ >8500
vdp_r6 equ >8600
vdp_r7 equ >8700

* VDP addresses.
vdpwa  equ  >8c02
vdpwd  equ  >8c00
vdprd  equ  >8800
vdpsta equ  >8802

* VDP table sizes.
screen_image_table_size                   equ >0300
color_table_size                          equ >0020
half_bitmap_color_table_size              equ >0800
bitmap_color_table_size                   equ >1800
pattern_descriptor_table_size             equ >0800
half_bitmap_pattern_descriptor_table_size equ >0800
bitmap_pattern_descriptor_table_size      equ >1800
sprite_attribute_table_size               equ >0080
sprite_descriptor_table_size              equ >0800

* VDP write address bits (with shifted versions to avoid problems with signed
* shift/division).
vdp_register_bit equ >8000
vdp_write_bit    equ >4000

vdp_register_bit_lsb equ >0080
vdp_write_bit_lsb    equ >0040

* Sprite attribute table values.
sprite_attribute_table_terminator equ >d0

* Sprite color attribute: early clock flag.
sprite_early_clock_flag  equ >80
sprite_early_clock_shift equ 32

* Macro: li instruction with a swapped word.
* IN #1: the register number.
* IN #2: the data.
    .defm li_swapped
    li   #1, (#2 % 256 * 256) + (#2 / 256)
    .endm

* Macro: li instruction with a constant color combination in the MSB.
* IN #1: the register number.
* IN #2: the foreground color.
* IN #3: the background color.
    .defm li_color
    li   #1, (#2 * 16 + #3) * 256
    .endm

* Macro: byte directive with a constant color combination.
* IN #1: the foreground color.
* IN #2: the background color.
    .defm byte_color
    byte #1 * 16 + #2
    .endm

* Macro: cache the vdpwa constant in the given register, to automatically get
*        more compact vdpwa macro calls later on.
* IN #1:  the register number.
* OUT #1: the vdpwa constant.
    .defm vdpwa_in_register
r_vdpwa equ #1
    li   r_vdpwa, vdpwa
    .endm

* Macro: cache the vdpwd constant in the given register, to automatically get
*        more compact vdpwd macro calls later on.
* IN #1:  the register number.
* OUT #1: the vdpwd constant.
    .defm vdpwd_in_register
r_vdpwd equ #1
    li   r_vdpwd, vdpwd
    .endm

* Macro: cache the vdprd constant in the given register, to automatically get
*        more compact vdprd macro calls later on.
* IN #1:  the register number.
* OUT #1: the vdprd constant.
    .defm vdprd_in_register
r_vdprd equ #1
    li   r_vdprd, vdprd
    .endm

* Macro: cache the vdpsta constant in the given register, to automatically get
*        more compact vdpsta macro calls later on.
* IN #1:  the register number.
* OUT #1: the vdpsta constant.
    .defm vdpsta_in_register
r_vdpsta equ #1
    li   r_vdpsta, vdpsta
    .endm

* Macro: write the given VDP address.
* IN #1: the register containing the VDP address
*        (including the register bit or write bit if necessary).
    .defm vdpwa_register
    .ifdef r_vdpwa
    swpb #1
    movb #1, *r_vdpwa
    swpb #1
    movb #1, *r_vdpwa
    .else
    swpb #1
    movb #1, @vdpwa
    swpb #1
    movb #1, @vdpwa
    .endif
    .endm

* Macro: write the given VDP address.
* IN #1: the register containing the VDP address, pre-swapped
*        (including the register bit or write bit if necessary).
    .defm vdpwa_swapped
    .ifdef r_vdpwa
    movb #1, *r_vdpwa
    swpb #1
    movb #1, *r_vdpwa
    .else
    movb #1, @vdpwa
    swpb #1
    movb #1, @vdpwa
    .endif
    .endm

* Macro: write the given VDP address.
* IN #1: the constant VDP address (including the register bit or write bit
*        if necessary).
* LOCAL r0
    .defm vdpwa_immediate
    .li_swapped    r0, #1
    .vdpwa_swapped r0
    .endm

* Macro: write the given value to the given VDP register.
* IN #1: the register number.
* IN #2: the register value.
* OUT r0: the VDP register data.
    .defm vdpwr
    li   r0, (#2) * 256 + vdp_register_bit_lsb + (#1)
    .vdpwa_swapped r0
    .endm

* Macro: write the given VDP address.
* IN #1: the constant VDP address or the register containing the VDP address
*        (including the register bit or write bit if necessary).
* LOCAL r0, for a constant VDP address.
    .defm vdpwa
    .ifdef #1
    .vdpwa_register #1
    .else
    .vdpwa_immediate #1
    .endif
    .endm

* Macro: write the given value to VDP memory at the current VDP address.
* IN #1: the source.
    .defm vdpwd
    .ifdef r_vdpwd
    movb #1, *r_vdpwd
    .else
    movb #1, @vdpwd
    .endif
    .endm

* Macro: read the value from VDP memory at the current VDP address.
* OUT #1: the destination.
    .defm vdprd
    .ifdef r_vdprd
    movb *r_vdprd, #1
    .else
    movb @vdprd, #1
    .endif
    .endm

* Macro: read the value from the VDP status register.
* OUT #1: the destination.
    .defm vdpsta
    .ifdef r_vdpsta
    movb *r_vdpsta, #1
    .else
    movb @vdpsta, #1
    .endif
    .endm

bitmap_mode    equ >02
external_video equ >01

* Macro: sets the VDP mode.
* IN #1: the mode (bitmap_mode | external_video).
* OUT r0: the VDP register data.
    .defm vdpwr_mode
    .vdpwr 0, #1
    .endm

vdp16k            equ >80
display_enable    equ >40
interrupt_enable  equ >20
text_mode         equ >10
multicolor_mode   equ >08
double_sprites    equ >02
magnified_sprites equ >01

* Macro: sets the VDP flags.
* IN #1: the flags (vdp16k | display_enable | interrupt_enable | text_mode |
*        multicolor_mode | double_sprites | magnified_sprites).
* OUT r0: the VDP register data.
    .defm vdpwr_flags
    .vdpwr 1, #1
    .endm

* Macro: sets the address of the VDP screen image table.
* IN #1: the address (a multiple of >400).
* OUT r0: the VDP register data.
    .defm vdpwr_screen_image_table
    .ifne #1 % >400, 0
    .error 'Screen image table address must be a multiple of >400'
    .endif
    .vdpwr 2, #1 / >400
    .endm

* Macro: sets the address of the VDP color table.
* IN #1: the address (a multiple of >40).
* IN #2: optionally, the constant bitmap_mode (to set the address mask).
* OUT r0: the VDP register data.
    .defm vdpwr_color_table
    .ifdef #2
      .ifne #1 % >2000, 0
      .error 'Color table address must be a >0000 or >2000 in bitmap mode'
      .endif
      .vdpwr 3, #1 / >40 | >7f
    .else
      .ifne #1 % >40, 0
      .error 'Color table address must be a multiple of >40'
      .endif
      .vdpwr 3, #1 / >40
      .endif
    .endm

* Macro: sets the address of the VDP pattern descriptor table.
* IN #1: the address (a multiple of >800).
* IN #2: optionally, the constant bitmap_mode (to set the address mask).
* OUT r0: the VDP register data.
    .defm vdpwr_pattern_descriptor_table
    .ifdef #2
      .ifne #1 % >2000, 0
      .error 'Pattern descriptor table address must be >0000 or >2000 in bitmap mode'
      .endif
      .vdpwr 4, #1 / >800 | >03
    .else
      .ifne #1 % >800, 0
      .error 'Pattern descriptor table address must be a multiple of >800'
      .endif
      .vdpwr 4, #1 / >800
      .endif
    .endm

* Macro: sets the address of the VDP sprite attribute table (list).
* IN #1: the address (a multiple of >80).
* OUT r0: the VDP register data.
    .defm vdpwr_sprite_attribute_table
    .ifne #1 % >80, 0
    .error 'Sprite attribute table address must be a multiple of >80'
    .endif
    .vdpwr 5, #1 / >80
    .endm

* Macro: sets the address of the VDP sprite descriptor table.
* Macro: sets the VDP background color.
* IN #1: the address (a multiple of >800).
* OUT r0: the VDP register data.
    .defm vdpwr_sprite_descriptor_table
    .ifne #1 % >800, 0
    .error 'Sprite descriptor table address must be a multiple of >800'
    .endif
    .vdpwr 6, #1 / >800
    .endm

* Macro: sets the VDP background color.
* IN #1: the color.
* OUT r0: the VDP register data.
    .defm vdpwr_background_color
    .vdpwr 7, #1
    .endm

* Macro: wait for a Vsync and clear the VDP Vsync status.
*        Note that VDP interrupts must be enabled for this to work
*        (with VDP register 1).
* LOCAL r12
    .defm wait_for_vsync
    clr  r12
!   tb   2                     ; Check the CRU interrupt bit (more reliable
    jeq  -!                    ; than checking the VDP status byte).
    .vdpsta r12                ; Clear the VDP status byte.
    .endm

* Macro: write a decimal number at the given VDP address.
* IN #1: the register that contains the value.
* IN #2: the target location of the last digit.
* IN #3: the character number of digit '0', e.g. 48.
* LOCAL r0
* LOCAL r1
* LOCAL r2
* LOCAL r3
    .defm write_decimal
    mov  #1, r1
    li   r3, >4000 + #2
    li   r2, 10

!   clr  r0                    ; Compute the least significant digit.
    div  r2, r0

    sla  r1, 8
    ai   r1, (#3) * 256        ; Add the '0' char to the digit.

    .vdpwa r3                  ; Write the address of the digit.
    dec  r3

    .vdpwd r1                  ; Write the digit.

    mov  r0, r1                ; Loop until the value is 0.
    jne  -!
    .endm
