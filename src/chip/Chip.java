/*-------------------------------	
 * Mateus de Suza Novaes - 2020
 * Chip-8 core class 	
 * Chip.java
 * ------------------------------
 */


/*--------CHIP-8_MEMORY_MAP------
 * 
 *  0x000-0x1FF Reserved for interpreter
 *  
 *  0x050-0x0A0 - Used for the built in 4x5 pixel font set (0-F)
 * 
 *  0x200-0xFFF Program ROM and WRAM  
 *  
 *-------------------------------
 */

package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class Chip {
	
	private char[] memory;		// The total memory is 4Kb, we can represent with a vector like that.
	private char[] V;	   	// CPU registers, V[0] - V[E] General purposes, V[F] carry flag, 16 in total. 
	private char   I; 	   	// Address register(most of the cases will only user 12 bits).
	private char   pc;	   	// Program counter, current memory position.
	
	private char[] stack;		// Stack with 16 levels, used to remember the current location before a jump or call.
	private int    sp;		// Stack pointer, to remember which level of the stack is used.
	
	private int    delay_timer;	
	private int    sound_timer;
	
	private byte[] keys;
	
	private byte[] display;
	
	private boolean needRedraw;
	
	public void initialize() {
		memory = new char[4096]; 
		V = new char[16]; 
		I = 0x0;
		pc = 0x200;
		
		stack = new char[16];
		sp = 0;
		
		delay_timer = 0;
		sound_timer = 0;
		
		keys = new byte[16];
		
		display = new byte[64 * 32]; // 2048 pixels on display
		// display[63] = 1;
		// display[127] = 1;
		// display[255] = 1;
		
		needRedraw = false;
		
		loadFontset(); // will load font_set into memory HEX 0x050 - 0x0A0  == DEC 80 - 160 
	}
	
	public void emulateCycle() {
		// Fetch Opcode
		char opcode = (char)(memory[pc] << 8 | memory[pc+1]); // bit shift 8 times and merge of two bytes into an opcode
		System.out.println(Integer.toHexString(opcode)); // print opcode
		// Decode Opcode
		switch(opcode & 0xF000) {
			
			case 0x0000:
				switch(opcode & 0x00ff) {
					
					case 0x00E0: // Clear the screen
						for(int i = 0; i < display.length; i++) {
							display[i] = 0;
						}
						pc += 2; // Next instruction 
						needRedraw = true;
						break;
						
					case 0x00EE: // Returns from subroutine
						sp --;
						pc = (char)(stack[sp] + 2);
						break;
						
					default: // 0NNN Calls RCA 1802 program at address NNN. Not necessary for most ROMs.
					break;	
				
				}
				break;
		
			case 0x1000: // 1NNN: Jumps to adress nnn
				int nnn = opcode & 0x0FFF;
				pc = (char)nnn;
				break;
				
		
			case 0x2000: // 2NNN: Calls subroutine at NNN
				stack[sp] = pc;
				sp++; // incremented the STACK
				pc = (char)(opcode & 0x0FFF);
				break;
				
			case 0x3000:{ // 3XNN: Skips next instruction if VX equals to NN
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x0FF);
				if(V[x] == nn) {
					pc += 4;
					
				}else {
					pc += 2;
				}
				break;
			}
			
			case 0x4000: // 4XNN: Skips the next instruction if VX != NN
				if(V[(opcode & 0x0f00) >> 8] != (opcode & 0x00ff)) {
					pc += 4;
				}else {
					pc += 2; 
				}
				break;
				
			case 0x5000: // 5XY0 Skips the next instruction  if VX equals VY
				if(V[(opcode & 0x0f00) >> 8] == V[(opcode & 0x00f0) >> 4])
					pc += 4;
				else
					pc += 2;
				break;
				
				
			case 0x6000: // 6XNN Sets VX to NN - LD Vx, byte - Load one byte to a register x
				int x = (opcode & 0x0F00) >> 8 ;
				V[x] = (char)(opcode & 0x00FF);
				pc += 2;
				break;
			
				
			case 0x7000: // 7XNN Adds NN to VX. (Carry flag is not changed) -  ADD Vx, byte
				int xx = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				V[xx] = (char)((V[xx] + nn) & 0xFF);
				pc += 2;
				break;
			
			case 0x8000:
				switch(opcode & 0x000F) {
				
					case 0x0000: // 8XY0: Sets Vx to the value of Vy
						V[(opcode & 0x0f00) >> 8] = V[(opcode & 0x00f0) >>4];
						pc += 2;
						break;
						
					case 0x0001: // 8XY1: Sets VX to VX or VY. (Bitwise OR operation)
						V[(opcode & 0x0f00) >> 8] = (char)((V[(opcode & 0x0f00) >> 8] | V[(opcode & 0x00f0) >> 4]) & 0xff);
						pc += 2;
					
					case 0x0002: // 8XY2: Sets VX to VX & VY
						V[(opcode & 0x0f00) >> 8] = (char)((V[(opcode & 0x0f00) >> 8] & V[(opcode & 0x00f0) >> 4]) & 0xff);
						pc += 2;
						break;
						
					case 0x0003: // 8XY3: Sets VX to VX xor VY.
						V[(opcode & 0x0f00) >> 8] = (char)((V[(opcode & 0x0f00) >> 8] ^ V[(opcode & 0x00f0) >> 4]) & 0xff);
						pc += 2;
						break;
						
					case 0x0004: // Adds VY to VX. VF is set to 1 when carry applies else to 0
						int x_x = (opcode & 0x0f00) >> 8;
						int y = (opcode & 0x00f0) >> 4;
						if(V[y] > 255 - V[x_x]) {
							V[0xF] = 1;
						}else {
							V[0xF] = 0;
						}
						V[x_x] = (char)((V[x_x] + V[y]) & 0xFF);
						pc += 2;
						break;
						
					case 0x0005: // 8XY5 VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
						if(V[(opcode & 0x0f00) >> 8] > V[(opcode & 0x00f0) >> 4]) {
							V[0xF] = 1; // no borrow
						}else {
							V[0xF] = 0; // there is a borrow;
						}
						V[(opcode & 0x0f00) >> 8] = ((char)(V[(opcode & 0x0f00) >> 8] - V[(opcode & 0x00f0) >> 4]));
						pc += 2;
						break;
						
					case 0x0006: // 8XY6: Stores the least significant bit of VX in VF and then shifts VX to the right by 1
						int x_x_ = (opcode & 0x0f00) >> 8;
						V[0xF] = (char)(V[x_x_] & 0x1);
						V[x_x_] = (char)(V[x_x_] >> 1);
						pc += 2;
						break;
						
					case 0x0007: // 8XY7 Sets VX to VY - VX. VF is set to 0 when there is a borrow, and 1 when there is not.
						int a = (opcode & 0x0f00) >> 8;
						int b = (opcode & 0x00f0) >> 4;
						
						if(V[a] > V[b])
							V[0xf] = 0;
						else
							V[0xf] = 1;
						
						V[a] = (char)((V[b] - V[a]) & 0xff);
						
					case 0x000E: // 8XYE Stores the most significant bit of VX in VF an then shifts VX to the left by one
						int c = (opcode & 0x0f00) >> 8;
						V[0xF] = (char)(V[c] & 0x80);
						V[c] = (char)(V[c] << 1);
						pc += 2;
						break;	
				}
				break;
			
			case 0x9000: // 9XY0 Skips the next instruction if VX doesn't equal VY. (Usually the next instruction is a jump to skip a code block)
				
				if(V[(opcode & 0x0f00) >> 8] != V[(opcode & 0x00f0) >> 4]) {
					pc += 4;
				}else {
					pc += 2;
				}
				
			case 0xA000: // ANNN: Set I to NNN - LD I, addr
				I = (char)(opcode & 0x0FFF);
				pc += 2; //two bytes - next instruction
				break;
				
			case 0xB000: // BNNN: Jumps to the address NNN plus V0
				pc = (char)((opcode & 0x0fff) + (V[0] & 0xff)); 
				break;
				
				
			case 0xC000: // CXNN: Set VX to a random number & nn
				int randomNumber = new Random().nextInt(256) & (opcode & 0x00ff);
				V[(opcode & 0x0f00) >> 8] = (char)randomNumber;
				pc += 2;
				break;
				
			case 0xD000:{ // DXYN: Draw a Sprite x,y size 8, n. Sprite is located at I - DRW Vx, Vy, nibble
				int X= V[(opcode & 0x0F00) >> 8];
				int y = V[(opcode & 0x00F0) >> 4];
				int height = opcode & 0x000F;
				
				V[0xF] = 0; // RESET REGISTER VF == CARRY FLAG
				
				for(int _y = 0; _y < height; _y++ ) {
					int line = memory[I + _y];
					for(int _x = 0; _x < 8; _x++) {
						int pixel = line &(0x80 >> _x);
						if(pixel != 0) {
							int totalX = X + _x;
							int totalY = y + _y;
							
							totalX %= 64;
							totalY %= 32;
							
							int index = totalY * 64 + totalX;
							
							if(display[index] == 1)
								V[0xF] = 1;
							
							display[index] ^= 1;
						}
					}
				}
				
				pc += 2;
				needRedraw = true;
				break;
			}
			
			case 0xE000:
				switch(opcode & 0x00ff) {
					case 0x009E: // EX9E Skip the next instruction if the key VX is pressed
						int xxx = (opcode & 0x0f00) >> 8;
							int key = V[xxx];
							if(keys[key] == 1) {
								pc += 4;
							}else {
								pc += 2;
							}
						break;
						
					case 0x00A1: // EXA1 Skip the next instruction if the key VX is not pressed
						int xxx1 = (opcode & 0x0f00) >> 8;
							int key1 = V[xxx1];
							if(keys[key1] == 0) {
								pc += 4;
							}else {
								pc += 2;
							}
						break;
				}
				break;
			
			case 0xf000:
				
				switch(opcode & 0x00ff) { 
				
				case 0x0007:{ // FX07: Set VX to the value of delay_timer
					V[(opcode & 0x0f00) >> 8] = (char)delay_timer;
					pc +=2;
					break;
					
				}
				
				case 0x000A: // FX0A A key press is awaited, and then stored in VX
					for(int i = 0; i < keys.length; i++) {
						if(keys[i] == 1) {
							V[(opcode & 0x0f00) >> 8] = (char)i;
							pc += 2;
							break;
						}
					}
					
				case 0x0015:{ // FX15: Sets delay timer to V[x];
					delay_timer = V[(opcode & 0x0f00) >> 8];
					pc += 2;
					break;
					
				}
				
				case 0x0018:{ // FX18: Set the sound timer to Vx
					sound_timer = V[(opcode & 0x0f00) >> 8];
					pc += 2;
					break;
					
				}
				
				case 0x001E:{ // FX1E: Adds Vx to I
					I = (char)(I + V[(opcode & 0x0f00) >> 8]);
					pc += 2;
					break;
					
				}
					
				
				case 0x0029:{ // Sets I to the location of the sprite for the character VX(Fontset)
					int character = V[(opcode & 0x0f00) >> 8];
					I = (char)(0x050 + (character * 5));
					pc += 2;
					
					break;
				}
				
				case 0x0033:{ // FX33 Store a binary-coded decimal value in I, I+1, I+2
					int value = V[(opcode & 0x0f00) >> 8];
					int hundreds = (value - (value % 100)) / 100;
					value = value - hundreds * 100;
					int tens = (value - (value % 10)) / 10;
					value = value - tens * 10;
					memory[I] = (char)hundreds;
					memory[I + 1] = (char)tens;
					memory[I + 2] = (char)value;
					
					pc += 2;
					break;
					
					}
				
				case 0x0055:{ // FX55 Stores V0 to VX in memory starting at adress I
					for(int i = 0; i <= (opcode & 0x0f00) >> 8; i++) {
						memory[I + i] = V[i];
					}
					pc += 2;
					break;
				}
				
				case 0x0065:{ // FX65 fills V0 to VX with values from I
					int z= (opcode & 0x0f00) >> 8;
					for(int i = 0; i <= z ; i++) {
						V[i] = memory[I + i];
					}
					I = (char)(I + z + 1);
					pc += 2;
					break;
				}
				
				}
				break;
				
			default:
				System.err.println(" Unsuported Opcode!!");
				// System.exit(0); Exit success
		}
		// Update timers
		if(sound_timer > 0) {
			sound_timer --;
			Audio.playSound("./beep-02.wav");
		}
			
		if(delay_timer > 0)
			delay_timer --;
	}
	
	public byte[] getDisplay() {
		return display;
	}

	public void removeDrawFlag() {
		needRedraw = false;
		
	}

	public boolean needsRedraw() {
		return needRedraw;
	}

	public void loadProgram(String file) {
		DataInputStream input = null;
		try {
			input = new DataInputStream(new FileInputStream(new File(file)));
			
			int offset = 0; // offset meaning is the distance of 2 points
			while(input.available() > 0) {
				memory[0x200 + offset] = (char)(input.readByte() & 0xFF);
				offset++;
			}
			System.err.println("Log: Program loaded into memory"); // not error
		
		}catch(IOException e) {
			e.printStackTrace();
			System.exit(0);
		}finally{
			if(input != null) {
				try {
					input.close();
				}catch(IOException ex) {
					
				}
			}
		}	
	}
	
	public void loadFontset() {
		System.err.println("Log: Fontset loaded into memory"); // not error
		for(int i = 0; i < ChipData.fontset.length; i++) {
			memory[0x50 + i] = (char)(ChipData.fontset[i] & 0xFF);
		}
	}
	
	public void setKeyBuffer(int[] keyBuffer) {
		for(int i = 0; i < keys.length; i++) {
			keys[i] = (byte)keyBuffer[i];
		}
	}
}
