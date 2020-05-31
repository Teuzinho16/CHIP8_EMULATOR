package emulator;

import chip.Chip; // Importing class Chip from package 'chip'

public class Main extends Thread {
	
	// atributes
	private Chip chip8;
	private ChipFrame frame;
	
	// constructor method
	public Main() {
		chip8 = new Chip();
		chip8.initialize();
		chip8.loadProgram("./pong2.c8"); // Loads the ROM
		frame = new ChipFrame(chip8);
	}
	
	public void run() {
		// 60 Hz == 60 Updates p/second
		// Emulation Loop
		while(true) {
			chip8.setKeyBuffer(frame.getKeyBuffer());
			// Emulate just 1 cycle
			chip8.emulateCycle();
			
			// If needsRedraw() return set/true -- flag
			// The screen will be updated
			if(chip8.needsRedraw()) {
				frame.repaint();
				chip8.removeDrawFlag(); 
			}
			try {
				Thread.sleep(4); // 1000 / 60 = 16 miliseconds
			}catch(InterruptedException e) {
				// Unthrown exception
			}
			
		}
	}
	
	public static void main(String[] args) {
		Main main = new Main();
		main.start(); //Method of Thread library
	}
}
