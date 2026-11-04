; ===== CRC24C  g(D)=D24+D23+D21+D20+D17+D15+D13+D12+D8+D4+D2+D+1  (L=24) =====
; message byte(s) are loaded below; result: R0:R1:R2 = CRC (also ENQUEUEd R0,R1,R2)
MOV R1,#0
MOV R2,#0
; ---------- message byte 1: 0x31 ----------
MOV R0,#0x31          ; R0 = CRC[23:16] = 0 XOR message byte
; --- bit 1/8 ---
PUSH R0
POP A
ADD A,R0              ; A = 2*S2, CY = feedback bit
PUSH A
POP R0
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = feedback bit b (0/1)
PUSH A
POP R3                ; R3 = b
PUSH R1
POP A
ADD A,R1              ; A = 2*S1, CY = bit shifted into S2
PUSH A
POP R1
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R0
PUSH A
POP R0                ; S2 = 2*S2 + carry
PUSH R2
POP A
ADD A,R2              ; A = 2*S0, CY = bit shifted into S1
PUSH A
POP R2
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R1
PUSH A
POP R1                ; S1 = 2*S1 + carry
PUSH R3
POP A                 ; A = b
ADD A,R3
ADD A,R3              ; A = 3b
PUSH A
POP R4
ADD A,R4
ADD A,R4              ; A = 9b
ADD A,R3
ADD A,R3              ; A = 11b = q
PUSH A
POP R5
ADD A,R5
ADD A,R3              ; A = 2q+b = b*0x17
XRL A,R2
PUSH A
POP R2                ; S0 ^= b*0x17
PUSH R5
POP A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
ADD A,R3              ; A = 16q+b = b*0xB1
PUSH A
POP R4
XRL A,R1
PUSH A
POP R1                ; S1 ^= b*0xB1
PUSH R4
POP A
ADD A,R3              ; A = 16q+2b = b*0xB2
XRL A,R0
PUSH A
POP R0                ; S2 ^= b*0xB2
; --- bit 2/8 ---
PUSH R0
POP A
ADD A,R0              ; A = 2*S2, CY = feedback bit
PUSH A
POP R0
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = feedback bit b (0/1)
PUSH A
POP R3                ; R3 = b
PUSH R1
POP A
ADD A,R1              ; A = 2*S1, CY = bit shifted into S2
PUSH A
POP R1
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R0
PUSH A
POP R0                ; S2 = 2*S2 + carry
PUSH R2
POP A
ADD A,R2              ; A = 2*S0, CY = bit shifted into S1
PUSH A
POP R2
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R1
PUSH A
POP R1                ; S1 = 2*S1 + carry
PUSH R3
POP A                 ; A = b
ADD A,R3
ADD A,R3              ; A = 3b
PUSH A
POP R4
ADD A,R4
ADD A,R4              ; A = 9b
ADD A,R3
ADD A,R3              ; A = 11b = q
PUSH A
POP R5
ADD A,R5
ADD A,R3              ; A = 2q+b = b*0x17
XRL A,R2
PUSH A
POP R2                ; S0 ^= b*0x17
PUSH R5
POP A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
ADD A,R3              ; A = 16q+b = b*0xB1
PUSH A
POP R4
XRL A,R1
PUSH A
POP R1                ; S1 ^= b*0xB1
PUSH R4
POP A
ADD A,R3              ; A = 16q+2b = b*0xB2
XRL A,R0
PUSH A
POP R0                ; S2 ^= b*0xB2
; --- bit 3/8 ---
PUSH R0
POP A
ADD A,R0              ; A = 2*S2, CY = feedback bit
PUSH A
POP R0
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = feedback bit b (0/1)
PUSH A
POP R3                ; R3 = b
PUSH R1
POP A
ADD A,R1              ; A = 2*S1, CY = bit shifted into S2
PUSH A
POP R1
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R0
PUSH A
POP R0                ; S2 = 2*S2 + carry
PUSH R2
POP A
ADD A,R2              ; A = 2*S0, CY = bit shifted into S1
PUSH A
POP R2
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R1
PUSH A
POP R1                ; S1 = 2*S1 + carry
PUSH R3
POP A                 ; A = b
ADD A,R3
ADD A,R3              ; A = 3b
PUSH A
POP R4
ADD A,R4
ADD A,R4              ; A = 9b
ADD A,R3
ADD A,R3              ; A = 11b = q
PUSH A
POP R5
ADD A,R5
ADD A,R3              ; A = 2q+b = b*0x17
XRL A,R2
PUSH A
POP R2                ; S0 ^= b*0x17
PUSH R5
POP A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
ADD A,R3              ; A = 16q+b = b*0xB1
PUSH A
POP R4
XRL A,R1
PUSH A
POP R1                ; S1 ^= b*0xB1
PUSH R4
POP A
ADD A,R3              ; A = 16q+2b = b*0xB2
XRL A,R0
PUSH A
POP R0                ; S2 ^= b*0xB2
; --- bit 4/8 ---
PUSH R0
POP A
ADD A,R0              ; A = 2*S2, CY = feedback bit
PUSH A
POP R0
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = feedback bit b (0/1)
PUSH A
POP R3                ; R3 = b
PUSH R1
POP A
ADD A,R1              ; A = 2*S1, CY = bit shifted into S2
PUSH A
POP R1
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R0
PUSH A
POP R0                ; S2 = 2*S2 + carry
PUSH R2
POP A
ADD A,R2              ; A = 2*S0, CY = bit shifted into S1
PUSH A
POP R2
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R1
PUSH A
POP R1                ; S1 = 2*S1 + carry
PUSH R3
POP A                 ; A = b
ADD A,R3
ADD A,R3              ; A = 3b
PUSH A
POP R4
ADD A,R4
ADD A,R4              ; A = 9b
ADD A,R3
ADD A,R3              ; A = 11b = q
PUSH A
POP R5
ADD A,R5
ADD A,R3              ; A = 2q+b = b*0x17
XRL A,R2
PUSH A
POP R2                ; S0 ^= b*0x17
PUSH R5
POP A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
ADD A,R3              ; A = 16q+b = b*0xB1
PUSH A
POP R4
XRL A,R1
PUSH A
POP R1                ; S1 ^= b*0xB1
PUSH R4
POP A
ADD A,R3              ; A = 16q+2b = b*0xB2
XRL A,R0
PUSH A
POP R0                ; S2 ^= b*0xB2
; --- bit 5/8 ---
PUSH R0
POP A
ADD A,R0              ; A = 2*S2, CY = feedback bit
PUSH A
POP R0
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = feedback bit b (0/1)
PUSH A
POP R3                ; R3 = b
PUSH R1
POP A
ADD A,R1              ; A = 2*S1, CY = bit shifted into S2
PUSH A
POP R1
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R0
PUSH A
POP R0                ; S2 = 2*S2 + carry
PUSH R2
POP A
ADD A,R2              ; A = 2*S0, CY = bit shifted into S1
PUSH A
POP R2
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R1
PUSH A
POP R1                ; S1 = 2*S1 + carry
PUSH R3
POP A                 ; A = b
ADD A,R3
ADD A,R3              ; A = 3b
PUSH A
POP R4
ADD A,R4
ADD A,R4              ; A = 9b
ADD A,R3
ADD A,R3              ; A = 11b = q
PUSH A
POP R5
ADD A,R5
ADD A,R3              ; A = 2q+b = b*0x17
XRL A,R2
PUSH A
POP R2                ; S0 ^= b*0x17
PUSH R5
POP A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
ADD A,R3              ; A = 16q+b = b*0xB1
PUSH A
POP R4
XRL A,R1
PUSH A
POP R1                ; S1 ^= b*0xB1
PUSH R4
POP A
ADD A,R3              ; A = 16q+2b = b*0xB2
XRL A,R0
PUSH A
POP R0                ; S2 ^= b*0xB2
; --- bit 6/8 ---
PUSH R0
POP A
ADD A,R0              ; A = 2*S2, CY = feedback bit
PUSH A
POP R0
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = feedback bit b (0/1)
PUSH A
POP R3                ; R3 = b
PUSH R1
POP A
ADD A,R1              ; A = 2*S1, CY = bit shifted into S2
PUSH A
POP R1
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R0
PUSH A
POP R0                ; S2 = 2*S2 + carry
PUSH R2
POP A
ADD A,R2              ; A = 2*S0, CY = bit shifted into S1
PUSH A
POP R2
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R1
PUSH A
POP R1                ; S1 = 2*S1 + carry
PUSH R3
POP A                 ; A = b
ADD A,R3
ADD A,R3              ; A = 3b
PUSH A
POP R4
ADD A,R4
ADD A,R4              ; A = 9b
ADD A,R3
ADD A,R3              ; A = 11b = q
PUSH A
POP R5
ADD A,R5
ADD A,R3              ; A = 2q+b = b*0x17
XRL A,R2
PUSH A
POP R2                ; S0 ^= b*0x17
PUSH R5
POP A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
ADD A,R3              ; A = 16q+b = b*0xB1
PUSH A
POP R4
XRL A,R1
PUSH A
POP R1                ; S1 ^= b*0xB1
PUSH R4
POP A
ADD A,R3              ; A = 16q+2b = b*0xB2
XRL A,R0
PUSH A
POP R0                ; S2 ^= b*0xB2
; --- bit 7/8 ---
PUSH R0
POP A
ADD A,R0              ; A = 2*S2, CY = feedback bit
PUSH A
POP R0
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = feedback bit b (0/1)
PUSH A
POP R3                ; R3 = b
PUSH R1
POP A
ADD A,R1              ; A = 2*S1, CY = bit shifted into S2
PUSH A
POP R1
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R0
PUSH A
POP R0                ; S2 = 2*S2 + carry
PUSH R2
POP A
ADD A,R2              ; A = 2*S0, CY = bit shifted into S1
PUSH A
POP R2
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R1
PUSH A
POP R1                ; S1 = 2*S1 + carry
PUSH R3
POP A                 ; A = b
ADD A,R3
ADD A,R3              ; A = 3b
PUSH A
POP R4
ADD A,R4
ADD A,R4              ; A = 9b
ADD A,R3
ADD A,R3              ; A = 11b = q
PUSH A
POP R5
ADD A,R5
ADD A,R3              ; A = 2q+b = b*0x17
XRL A,R2
PUSH A
POP R2                ; S0 ^= b*0x17
PUSH R5
POP A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
ADD A,R3              ; A = 16q+b = b*0xB1
PUSH A
POP R4
XRL A,R1
PUSH A
POP R1                ; S1 ^= b*0xB1
PUSH R4
POP A
ADD A,R3              ; A = 16q+2b = b*0xB2
XRL A,R0
PUSH A
POP R0                ; S2 ^= b*0xB2
; --- bit 8/8 ---
PUSH R0
POP A
ADD A,R0              ; A = 2*S2, CY = feedback bit
PUSH A
POP R0
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = feedback bit b (0/1)
PUSH A
POP R3                ; R3 = b
PUSH R1
POP A
ADD A,R1              ; A = 2*S1, CY = bit shifted into S2
PUSH A
POP R1
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R0
PUSH A
POP R0                ; S2 = 2*S2 + carry
PUSH R2
POP A
ADD A,R2              ; A = 2*S0, CY = bit shifted into S1
PUSH A
POP R2
MOV A,#1
MOV R7,#0
SUBB A,R7             ; A = 1 - CY
MOV R7,#1
XRL A,R7              ; A = carry
ADD A,R1
PUSH A
POP R1                ; S1 = 2*S1 + carry
PUSH R3
POP A                 ; A = b
ADD A,R3
ADD A,R3              ; A = 3b
PUSH A
POP R4
ADD A,R4
ADD A,R4              ; A = 9b
ADD A,R3
ADD A,R3              ; A = 11b = q
PUSH A
POP R5
ADD A,R5
ADD A,R3              ; A = 2q+b = b*0x17
XRL A,R2
PUSH A
POP R2                ; S0 ^= b*0x17
PUSH R5
POP A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
PUSH A
POP R7
ADD A,R7              ; A = 2*A
ADD A,R3              ; A = 16q+b = b*0xB1
PUSH A
POP R4
XRL A,R1
PUSH A
POP R1                ; S1 ^= b*0xB1
PUSH R4
POP A
ADD A,R3              ; A = 16q+2b = b*0xB2
XRL A,R0
PUSH A
POP R0                ; S2 ^= b*0xB2
ENQUEUE R0
ENQUEUE R1
ENQUEUE R2
HALT
