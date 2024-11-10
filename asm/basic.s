#
# Just a few basic instructions to watch the pipeline going in ModelSim
#

#	.word   40
#	addi	r1 = r0, 0

#	addi	r1 = r0, 15

#	addi	r2 = r0, 4
#	addi	r3 = r0, 3
#	nop
#	add	r4 = r3, r2
#	halt
#	nop
#	nop
#	nop

#
# Test program with load and store instructions
#

    .word   52                 # Instruction 0: NOP (assuming opcode 0x00)
    addi    r1 = r0, 255        # Instruction 1: R1 = 255
    addi    r1 = r0, 15         # Instruction 2: R1 = 15
    addi    r2 = r0, 4          # Instruction 3: R2 = 4
    addi    r3 = r0, 3          # Instruction 4: R3 = 3
    add     r4 = r2, r3         # Instruction 5: R4 = R2 + R3 = 7
    lwc    r5 = [r4]           # Instruction 6: Load from address in R4 into R5
    swc   [r5] = r1           # Instruction 7: Store value from R1 into address in R5
    halt                        # Instruction 8: Halt the simulator
    nop                         # Instruction 9: NOP
    nop                         # Instruction 10: NOP
    nop                         # Instruction 11: NOP
    nop

