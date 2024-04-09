* Useful assembly definitions for the TI-99/4A home computer.
*
* Copyright (c) 2024 Eric Lafortune
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
* Definitions and macros for accessing the CPU and CPU RAM.
*
* The CPU and CPU RAM layout are documented in
*     http://www.unige.ch/medecine/nouspikel/ti99/tms9900.htm
*     http://www.unige.ch/medecine/nouspikel/ti99/roms.htm
*     http://www.unige.ch/medecine/nouspikel/ti99/padram.htm
******************************************************************************

* Registers reside in a workspace in RAM.
workspace_size equ >0020

* BLWP vectors.
reset_vector equ >0000
isr_vector   equ >0004
load_vector  equ >fffc

* Internal fast scratchpad RAM.
scratchpad      equ >8300
scratchpad_size equ >0100
scratchpad_end  equ scratchpad + scratchpad_size

* Typical external solid state cartridge ROM/RAM.
module_memory       equ >6000
module_memory_size  equ >2000
module_memory_end   equ >8000

* Typical base address for module memory bank selection
* (copying a byte at >6000, >6002,..., >7ffe).
module_bank_selection equ >6000
module_bank_count     equ >1000
module_bank_increment equ 2
module_bank_end       equ >8000

* Macro: switch to the specified memory bank.
* IN #1: the operand containing the address of the memory bank (@>6000,
*        @>6002,..., @>7ffe, or *r0,...)
    .defm switch_bank
    movb #1, #1
    .endm
