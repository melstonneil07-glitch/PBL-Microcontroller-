#!/usr/bin/env python3
"""Generate CRC24C assembly for the MS51FB9AE simulator editor.

Uses ONLY the simulator's instructions (MOV A/Rn,#d  ADD/SUBB/XRL A,Rn  INC  PUSH/POP  ENQUEUE  HALT).
There is no shift, AND or conditional jump, so the bit-serial CRC is unrolled and made branch-free:
  * 'ADD' of a value to itself doubles it (shift left) and leaves the shifted-out bit in CY
  * 'SUBB A,Rn' consumes CY, which is how CY is turned into the number 0 or 1
  * feedback*POLY is built as b*0xB2, b*0xB1, b*0x17 with b in {0,1} (see comments)
Polynomial: D24+D23+D21+D20+D17+D15+D13+D12+D8+D4+D2+D+1  ->  0x1B2B117 (low 24 bits 0xB2B117)
Registers: R0:R1:R2 = CRC (MSB..LSB), R3 = feedback bit, R4/R5 = temps, R7 = scratch.
"""
import sys

def gen(message):
    L = []
    def i(s, c=None): L.append(s if c is None else f"{s:<22}; {c}")
    def cp(dst, src, c=None):          # dst <- src   (via the stack: no MOV Rn,Rn in the ISA)
        i(f"PUSH {src}"); i(f"POP {dst}", c)
    def dbl_a():                       # A <- 2*A, CY <- old bit 7
        i("PUSH A"); i("POP R7"); i("ADD A,R7", "A = 2*A")
    def cy_to_a(c):                    # A <- CY (0/1), clobbers R7; CY ends up 0
        i("MOV A,#1"); i("MOV R7,#0"); i("SUBB A,R7", "A = 1 - CY")
        i("MOV R7,#1"); i("XRL A,R7", c)

    i("; ===== CRC24C  g(D)=D24+D23+D21+D20+D17+D15+D13+D12+D8+D4+D2+D+1  (L=24) =====")
    i("; message byte(s) are loaded below; result: R0:R1:R2 = CRC (also ENQUEUEd R0,R1,R2)")
    i("MOV R1,#0"); i("MOV R2,#0")
    for n, byte in enumerate(message):
        i(f"; ---------- message byte {n+1}: 0x{byte:02X} ----------")
        if n == 0:
            i(f"MOV R0,#0x{byte:02X}", "R0 = CRC[23:16] = 0 XOR message byte")
        else:
            i(f"MOV R7,#0x{byte:02X}"); cp("A", "R0"); i("XRL A,R7"); cp("R0", "A", "CRC[23:16] ^= message byte")
        for bit in range(8):
            i(f"; --- bit {bit+1}/8 ---")
            # S2 doubling -> CY = feedback bit
            cp("A", "R0"); i("ADD A,R0", "A = 2*S2, CY = feedback bit")
            cp("R0", "A")
            cy_to_a("A = feedback bit b (0/1)")
            cp("R3", "A", "R3 = b")
            # S1 shift, carry into S2
            cp("A", "R1"); i("ADD A,R1", "A = 2*S1, CY = bit shifted into S2")
            cp("R1", "A"); cy_to_a("A = carry"); i("ADD A,R0"); cp("R0", "A", "S2 = 2*S2 + carry")
            # S0 shift, carry into S1
            cp("A", "R2"); i("ADD A,R2", "A = 2*S0, CY = bit shifted into S1")
            cp("R2", "A"); cy_to_a("A = carry"); i("ADD A,R1"); cp("R1", "A", "S1 = 2*S1 + carry")
            # feedback * 0xB2B117, using q = 11*b:  0x17 = 2q+1, 0xB1 = 16q+1, 0xB2 = 16q+2
            cp("A", "R3", "A = b")
            i("ADD A,R3"); i("ADD A,R3", "A = 3b"); cp("R4", "A")
            i("ADD A,R4"); i("ADD A,R4", "A = 9b"); i("ADD A,R3"); i("ADD A,R3", "A = 11b = q")
            cp("R5", "A")
            i("ADD A,R5"); i("ADD A,R3", "A = 2q+b = b*0x17"); i("XRL A,R2"); cp("R2", "A", "S0 ^= b*0x17")
            cp("A", "R5")
            for _ in range(4): dbl_a()
            i("ADD A,R3", "A = 16q+b = b*0xB1"); cp("R4", "A"); i("XRL A,R1"); cp("R1", "A", "S1 ^= b*0xB1")
            cp("A", "R4"); i("ADD A,R3", "A = 16q+2b = b*0xB2"); i("XRL A,R0"); cp("R0", "A", "S2 ^= b*0xB2")
    i("ENQUEUE R0"); i("ENQUEUE R1"); i("ENQUEUE R2")
    i("HALT")
    return "\n".join(L) + "\n"

if __name__ == "__main__":
    msg = [int(x, 0) for x in sys.argv[1:]] or [0x31]
    sys.stdout.write(gen(msg))
