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
* Definitions and macros for accessing the speech synthesizer.
*
* Programming it is documented in
*     http://www.unige.ch/medecine/nouspikel/ti99/speech.htm
******************************************************************************

* Speech addresses.
spchrd equ  >9000
spchwt equ  >9400

* Macro: cache the spchrd constant in the given register, to automatically get
*        more compact spchrd macro calls later on.
* IN #1:  the register number.
* OUT #1: the spchrd constant.
    .defm spchrd_in_register
r_spchrd equ #1
    li   r_spchrd, spchrd
    .endm

* Macro: read a value from the speech synthesizer.
* OUT #1: the destination.
    .defm spchrd
    .ifdef r_spchrd
    movb *r_spchrd, #1
    .else
    movb @spchrd, #1
    .endif
    .endm

* Macro: cache the spchwt constant in the given register, to automatically get
*        more compact spchwt macro calls later on.
* IN #1:  the register number.
* OUT #1: the spchwt constant.
    .defm spchwt_in_register
r_spchwt equ #1
    li   r_spchwt, spchwt
    .endm

* Macro: write the given value to the speech synthesizer.
* IN #1: the source.
    .defm spchwt
    .ifdef r_spchwt
    movb #1, *r_spchwt
    .else
    movb #1, @spchwt
    .endif
    .endm
