package com.mayo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class Chip8 {
  private final byte[] ram = new byte[4096];
  private final byte[][] display = new byte[64][32];
  private final boolean[] keypad = new boolean[16];
  private short programCounter;
  private short index;
  private Deque<Short> stack = new ArrayDeque<Short>();
  private byte[] V = new byte[16];
  private byte delayTimer;
  private byte soundTimer;
  private volatile boolean shouldDraw;

  private static final short[] font = {
      0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
      0x20, 0x60, 0x20, 0x20, 0x70, // 1
      0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
      0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
      0x90, 0x90, 0xF0, 0x10, 0x10, // 4
      0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
      0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
      0xF0, 0x10, 0x20, 0x40, 0x40, // 7
      0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
      0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
      0xF0, 0x90, 0xF0, 0x90, 0x90, // A
      0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
      0xF0, 0x80, 0x80, 0x80, 0xF0, // C
      0xE0, 0x90, 0x90, 0x90, 0xE0, // D
      0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
      0xF0, 0x80, 0xF0, 0x80, 0x80 // F
  };

  public Chip8(String program_name) {
    for (int i = 0; i < font.length; ++i) {
      ram[i] = (byte) font[i];
    }
    loadProgram(program_name);
    programCounter = 0x200;
  }

  public void setDraw(boolean shouldDraw) {
    this.shouldDraw = shouldDraw;
  }

  public boolean getDraw() {
    return shouldDraw;
  }

  public void print() {

    for (int i = 0; i < 64; i++) {
      for (int j = 0; j < 32; j++) {
        // System.out.printf("%x ", display[i][j]);
      }
    }
    System.out.printf("\n\n");
  }

  public byte[][] getDisplay() {
    return display;
  }

  public void run() {
    short instruction = fetch();
    decode_execute(instruction);
  }

  private void loadProgram(String program_name) {
    File file = new File(program_name);
    try {
      if (file.exists() && file.isFile()) {
        byte[] bytes = Files.readAllBytes(file.toPath());
        int i = 0x0200;
        for (byte bits : bytes) {
          ram[i++] = bits;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private short fetch() {
    short instruction = (short) (ram[programCounter] << 8 | (ram[programCounter + 1] & 0xFF));
    programCounter += 2;
    return instruction;
  }

  private void decode_execute(short instruction) {
    byte op_nibble = (byte) ((instruction & 0xF000) >> 12);
    byte X = (byte) ((instruction & 0x0F00) >> 8);
    byte Y = (byte) ((instruction & 0x00F0) >> 4);
    byte N = (byte) (instruction & 0x000F);
    byte NN = (byte) (instruction & 0x00FF);
    short NNN = (short) (instruction & 0x0FFF);
    System.out.printf(
        "PC: %4X, instruction: %4X, op_nibble: %2X, X: %2X, Y: %2X, N: "
            + "%2X, NN: %2X, NNN: %2X\n",
        programCounter, instruction, op_nibble, X, Y, N, NN, NNN);
    switch (op_nibble) {
      case (0): {
        switch (N) {
          case (0x0): {
            CLS();
            break;
          }
          case (0xE): {
            RET();
            break;
          }
        }
        break;
      }
      case (0x1): {
        JP_NNN(NNN);
        break;
      }
      case (0x2): {
        CALL_NNN(NNN);
        break;
      }
      case (0x3): {
        SE_XNN(X, NN);
        break;
      }
      case (0x4): {
        SNE_XNN(X, NN);
        break;
      }
      case (0x5): {
        SE_XY(X, N);
        break;
      }
      case (0x6): {
        LD_XNN(X, NN);
        break;
      }
      case (0x7): {
        ADD_XNN(X, NN);
        break;
      }
      case (0x8): {
        switch (N) {
          case (0x0): {
            LD_XY(X, Y);
            break;
          }
          case (0x1): {
            OR_XY(X, Y);
            break;
          }
          case (0x2): {
            AND_XY(X, Y);
            break;
          }
          case (0x3): {
            XOR_XY(X, Y);
            break;
          }
          case (0x4): {
            ADD_XY(X, Y);
            break;
          }
          case (0x5): {
            SUB_XY(X, Y);
            break;
          }
          case (0x6): {
            SHR_XIY(X, Y);
            break;
          }
          case (0x7): {
            SUBN_XY(X, Y);
            break;
          }
          case (0xE): {
            SHL_XIY(X, Y);
            break;
          }
        }
        break;
      }
      case (0x9): {
        SNE_XY(X, Y);
        break;
      }
      case (0xA): {
        LD_NNN(NNN);
        break;
      }
      case (0xB): {
        JP_V0_NNN(NNN);
        break;
      }
      case (0xC): {
        RND_XNN(X, NN);
        break;
      }
      case (0xD): {
        DRW_XYN(X, Y, N);
        break;
      }
      case (0xE): {
        switch (N) {
          case (0x1): {
            SKP_X(X);
            break;
          }
          case (0xE): {
            SKNP_X(X);
            break;
          }
        }
        break;
      }
      case (0xF): {
        switch (NN) {
          case (0x07): {
            LD_XDT(X);
            break;
          }
          case (0x0A): {
            LD_XN(X, N);
            break;
          }
          case (0x15): {
            LD_DTX(X);
            break;
          }
          case (0x18): {
            LD_STX(X);
            break;
          }
          case (0x1E): {
            ADD_IX(X);
            break;
          }
          case (0x29): {
            LD_FX(X);
            break;
          }
          case (0x33): {
            LD_BX(X);
            break;
          }
          case (0x55): {
            LD_IIX(X);
            break;
          }
          case (0x65): {
            LD_XII(X);
            break;
          }
        }
        break;
      }
    }
  }

  private void CLS() {
    for (int i = 0; i < display.length; ++i) {
      for (int j = 0; j < display[0].length; j++) {
        display[i][j] = 0x00;
      }
    }
  }

  private void RET() {
    programCounter = stack.pop();
  }

  private void JP_NNN(final short NNN) {
    programCounter = (short) (NNN & 0xFFF);
  }

  private void CALL_NNN(final short NNN) {
    stack.push(programCounter);
    programCounter = (short) (NNN & 0xFFF);
  }

  private void SE_XNN(final byte X, final byte NN) {
    if (V[X] == NN) {
      programCounter += 2;
    }
  }

  private void SNE_XNN(final byte X, final byte NN) {
    if (V[X] != NN) {
      programCounter += 2;
    }
  }

  private void SE_XY(final byte X, final byte Y) {
    if (V[X] == V[Y]) {
      programCounter += 2;
    }
  }

  private void LD_XNN(final byte X, final byte NN) {
    V[X] = NN;
  }

  private void ADD_XNN(final byte X, final byte NN) {
    V[X] += NN;
  }

  private void LD_XY(final byte X, final byte Y) {
    V[X] = V[Y];
  }

  private void OR_XY(final byte X, final byte Y) {
    V[X] = (byte) (V[X] | V[Y]);
  }

  private void AND_XY(final byte X, final byte Y) {
    V[X] = (byte) (V[X] & V[Y]);
  }

  private void XOR_XY(final byte X, final byte Y) {
    V[X] = (byte) (V[X] ^ V[Y]);
  }

  private void ADD_XY(final byte X, final byte Y) {
    int sum = (V[X] & 0xFF) + (V[Y] & 0xFF);
    V[X] = (byte) (sum & 0xFF);
    V[0xF] = (byte) ((sum > 255) ? 1 : 0);
  }

  private void SUB_XY(final byte X, final byte Y) {
    int x = (V[X] & 0xFF);
    int y = (V[Y] & 0xFF);
    V[X] = (byte) ((x - y) & 0xFF);
    V[0xF] = (byte) ((x >= y) ? 1 : 0);
  }

  private void SHR_XIY(final byte X, final byte Y) {
    int x = V[X];
    V[X] >>= 1;
    V[0xF] = (byte) (((x & 0x1) == 1) ? 1 : 0);
  }

  private void SUBN_XY(final byte X, final byte Y) {
    int x = (V[X] & 0xFF);
    int y = (V[Y] & 0xFF);
    V[X] = (byte) ((y - x) & 0xFF);
    V[0xF] = (byte) ((y >= x) ? 1 : 0);
  }

  private void SHL_XIY(final byte X, final byte Y) {
    int x = V[X];
    V[X] <<= 1;
    V[0xF] = (byte) (((x & 0x80) == 0x80) ? 1 : 0);
  }

  private void SNE_XY(final byte X, final byte Y) {
    if (V[X] != V[Y]) {
      programCounter += 2;
    }
  }

  private void LD_NNN(final short NNN) {
    index = NNN;
  }

  private void JP_V0_NNN(final short NNN) {
    programCounter = (byte) (NNN + V[0x0]);
  }

  private void RND_XNN(final byte X, final byte NN) {
    int randomNum = new Random().nextInt(256);
    V[X] = (byte) (randomNum & NN);
  }

  private void DRW_XYN(final byte X, final byte Y, final short N) {
    short x_coord = (short) (V[X] & 0xFF);
    short y_coord = (short) (V[Y] & 0xFF);
    short sprite;
    short value;
    V[0xF] = 0;
    for (short i = 0; i < N; ++i) {
      int display_y = (y_coord + i) % 32;
      sprite = ram[index + i];
      for (short j = 0; j < 8; ++j) {
        int display_x = (x_coord + j) % 64;
        value = (short) ((sprite & 0x80) >>> 7);
        if (value == 1) {
          display[display_x][display_y] ^= 1;
          if (display[display_x][display_y] == 0) {
            V[0xF] = 1;
          }
        }
        sprite = (short) ((sprite << 1) & 0xFF);
      }
    }
    setDraw(true);
  }

  private void SKP_X(final byte X) {
    if (keypad[X]) {
      programCounter += 2;
    }
  }

  private void SKNP_X(final byte X) {
    if (!keypad[X]) {
      programCounter += 2;
    }
  }

  private void LD_XDT(final byte X) {
    V[X] = delayTimer;
  }

  private void LD_XN(final byte X, final short N) {
    boolean isPressed = false;
    int val = 0;
    for (int i = 0; i < 16; i++) {
      if (keypad[i]) {
        isPressed = true;
        val = i;
      }
    }
    if (!isPressed) {
      programCounter -= 2;
    } else {
      V[X] = (byte) val;
    }
  }

  private void LD_DTX(final byte X) {
    delayTimer = (byte) (V[X] & 0xFF);
  }

  private void LD_STX(final byte X) {
    soundTimer = (byte) (V[X] & 0xFF);
  }

  private void ADD_IX(final byte X) {
    index = (short) ((index + V[X]) & 0xFFFF);
  }

  private void LD_FX(final byte X) {
    int val = V[X] & 0xFF;
    index = (short) (val * 5);
  }

  private void LD_BX(final byte X) {
    int num = V[X] & 0xFF;
    int num_100 = (num % 1000) / 100;
    int num_10 = (num % 100) / 10;
    int num_1 = num % 10;
    ram[index] = (byte) (num_100 & 0xFF);
    ram[index + 1] = (byte) (num_10 & 0xFF);
    ram[index + 2] = (byte) (num_1 & 0xFF);
  }

  private void LD_IIX(final byte X) {
    for (int i = 0; i <= X; i++) {
      ram[index + i] = (byte) (V[i] & 0xFF);
    }
  }

  private void LD_XII(final byte X) {
    for (int i = 0; i <= X; i++) {
      V[i] = (byte) (ram[index + i] & 0xFF);
    }
  }
}
