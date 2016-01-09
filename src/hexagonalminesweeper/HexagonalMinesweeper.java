/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hexagonalminesweeper;

import java.awt.Font;
import java.util.Random;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

public class HexagonalMinesweeper {

    public static int pixel = 10;
    public static int[][] selectedHexagons = new int[200][100];
    public static int[][] values1 = new int[200][100];
    public static int[][] values2 = new int[200][100];
    public static boolean vertexUpwards = true;
    public static boolean covered = true;
    public static double menu = 1;
    public static double time = 0;
    public static double initialTime = 0;
    public static int savedTime = 0;

    public static int mineNumber = 500;
    public static int tileNumber;
    public static int flagNumber;
    public static Random r = new Random();
    public static int gameStage;
    public static boolean gameStart = false;
    public static int timerStart = 1;

    public static boolean leftClick, rightClick, allMines;

    public static void main(String[] args) {
        renderGL();
        tileCount();
        layMines();

        Font titleFont = new Font("Times New Roman", Font.BOLD, 40);
        TrueTypeFont trueTypeTitleFont = new TrueTypeFont(titleFont, false);

        Font bylineFont = new Font("Times New Roman", Font.BOLD, 20);
        TrueTypeFont trueTypeBylineFont = new TrueTypeFont(bylineFont, false);

        gameLoop(trueTypeTitleFont, trueTypeBylineFont);
    }

    public static void gameLoop(TrueTypeFont trueTypeTitleFont, TrueTypeFont trueTypeBylineFont) {
        while (!Display.isCloseRequested()) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            while (Keyboard.next()) {
                if (Keyboard.getEventKeyState()) {
                    if (Keyboard.getEventKey() == Keyboard.KEY_F) {
                        setDisplayMode(800, 600, !Display.isFullscreen());
                    }
                }
            }

            if (!gameStart) {
                titleScreen(trueTypeTitleFont, trueTypeBylineFont);
                if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                    gameStart = true;
                }
            } else {
                int dwheel = Mouse.getDWheel();
                if (dwheel < 0) {
                    if (mineNumber >= 1) {
                        mineNumber--;
                        if (mineNumber < 0) {
                            mineNumber = 0;
                        }
                        values1 = new int[200][100];
                        values2 = new int[200][100];
                        layMines();
                        if (covered) {
                            selectedHexagons = new int[200][100];
                            timerStart = 1;
                        } else {
                            uncover();
                            timerStart = 0;
                        }
                    }
                } else if (dwheel > 0) {
                    mineNumber++;
                    values1 = new int[200][100];
                    values2 = new int[200][100];
                    layMines();
                    if (covered) {
                        selectedHexagons = new int[200][100];
                        timerStart = 1;
                    } else {
                        uncover();
                        timerStart = 0;
                    }
                }

                time = System.nanoTime() / 1000000000;

                mouseClick();
                keyboardInput();
                flood();
                tileCount();
                checkWinLose();
                draw();
                drawMenu(trueTypeBylineFont);

            }
            Display.update();
            Display.sync(60);

        }
        Display.destroy();

    }

    public static void renderGL() {
        try {
            Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(800, 600));
            Display.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClearDepth(1);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glViewport(0, 0, 800, 600);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 800, 600, 0, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        Display.setVSyncEnabled(true);
    }

    public static void setDisplayMode(int width, int height, boolean fullscreen) {

        // return if requested DisplayMode is already set
        if ((Display.getDisplayMode().getWidth() == width)
                && (Display.getDisplayMode().getHeight() == height)
                && (Display.isFullscreen() == fullscreen)) {
            return;
        }

        try {
            DisplayMode targetDisplayMode = null;

            if (fullscreen) {
                DisplayMode[] modes = Display.getAvailableDisplayModes();
                int freq = 0;

                for (int i = 0; i < modes.length; i++) {
                    DisplayMode current = modes[i];

                    if ((current.getWidth() == width) && (current.getHeight() == height)) {
                        if ((targetDisplayMode == null) || (current.getFrequency() >= freq)) {
                            if ((targetDisplayMode == null) || (current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
                                targetDisplayMode = current;
                                freq = targetDisplayMode.getFrequency();
                            }
                        }

                        // if we've found a match for bpp and frequence against the
                        // original display mode then it's probably best to go for this one
                        // since it's most likely compatible with the monitor
                        if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel())
                                && (current.getFrequency() == Display.getDesktopDisplayMode().getFrequency())) {
                            targetDisplayMode = current;
                            break;
                        }
                    }
                }
            } else {
                targetDisplayMode = new DisplayMode(width, height);
            }

            if (targetDisplayMode == null) {
                System.out.println("Failed to find value mode: " + width + "x" + height + " fs=" + fullscreen);
                return;
            }

            Display.setDisplayMode(targetDisplayMode);
            Display.setFullscreen(fullscreen);

        } catch (LWJGLException e) {
            System.out.println("Unable to setup mode " + width + "x" + height + " fullscreen=" + fullscreen + e);
        }
    }

    public static void layMines() {

        int xi;
        int yi;
        float DEG2RAD = (float) (3.14159 / 180);

        tileCount();

        int m;

        if (tileNumber < mineNumber) {
            m = tileNumber;
        } else {
            m = mineNumber;
        }

        if (vertexUpwards) {

            someLabel:
            while (m > 0) {
                xi = r.nextInt(200);
                yi = r.nextInt(100);

                if (allMines) {
                    break;
                }

                for (int i = 90; i < 450; i = i + 60) {

                    float degInRad = i * DEG2RAD;

                    if (yi % 2 == 0) {
                        if ((Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2) >= 600 || (Math.cos(degInRad) * pixel / 2 + xi * pixel + pixel / 2) >= 800) {
                            continue someLabel;
                        }
                    } else {
                        if ((Math.sin(degInRad) * pixel / 2 + (yi) * pixel * 0.86 + pixel / 2) >= 600 || (Math.cos(degInRad) * pixel / 2 + (xi + 0.5) * pixel + pixel / 2) >= 800) {
                            continue someLabel;
                        }
                    }
                }

                if (values1[xi][yi] != -1) {
                    values1[xi][yi] = -1;
                    m--;
                }
            }

            int tileValue = 0;

            for (xi = 0; xi < 200; xi++) {

                someLabel:
                for (yi = 0; yi < 100; yi++) {

                    if (values1[xi][yi] != -1) {

                        for (int i = 90; i < 450; i = i + 60) {

                            float degInRad = i * DEG2RAD;

                            if (yi % 2 == 0) {
                                if ((Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2) >= 600 || (Math.cos(degInRad) * pixel / 2 + xi * pixel + pixel / 2) >= 800) {
                                    continue someLabel;
                                }
                            } else {
                                if ((Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2) >= 600 || (Math.cos(degInRad) * pixel / 2 + (xi + 0.5) * pixel + pixel / 2) >= 800) {
                                    continue someLabel;
                                }
                            }
                        }

                        if (yi % 2 == 1) {

                            if (xi + 1 < 200) {
                                if (values1[xi + 1][yi] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi + 1 < 200 && yi - 1 >= 0) {
                                if (values1[xi + 1][yi - 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi + 1 < 200 && yi + 1 < 100) {
                                if (values1[xi + 1][yi + 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (yi + 1 < 100) {
                                if (values1[xi][yi + 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi - 1 >= 0) {
                                if (values1[xi - 1][yi] == -1) {
                                    tileValue++;
                                }
                            }
                            if (yi - 1 >= 0) {
                                if (values1[xi][yi - 1] == -1) {
                                    tileValue++;
                                }
                            }

                        } else {
                            if (yi - 1 >= 0) {
                                if (values1[xi][yi - 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi + 1 < 200) {
                                if (values1[xi + 1][yi] == -1) {
                                    tileValue++;
                                }
                            }
                            if (yi + 1 < 100) {
                                if (values1[xi][yi + 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi - 1 >= 0 && yi + 1 < 100) {
                                if (values1[xi - 1][yi + 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi - 1 >= 0) {
                                if (values1[xi - 1][yi] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi - 1 >= 0 && yi - 1 >= 0) {
                                if (values1[xi - 1][yi - 1] == -1) {
                                    tileValue++;
                                }
                            }
                        }

                        values1[xi][yi] = tileValue;
                        tileValue = 0;

                    }

                }
            }
        } else {

            someLabel:
            while (m > 0) {
                xi = r.nextInt(200);
                yi = r.nextInt(100);

                if (allMines) {
                    break;
                }

                for (int i = 0; i < 360; i = i + 60) {

                    float degInRad = i * DEG2RAD;

                    if (xi % 2 == 0) {
                        if ((Math.cos(degInRad) * pixel / 2 + xi * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel + pixel / 2) >= 600) {
                            continue someLabel;
                        }
                    } else {
                        if ((Math.cos(degInRad) * pixel / 2 + (xi) * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + (yi + 0.5) * pixel + pixel / 2) >= 600) {
                            continue someLabel;
                        }
                    }
                }

                if (values2[xi][yi] != -1) {
                    values2[xi][yi] = -1;
                    m--;
                }
            }

            int tileValue = 0;

            for (xi = 0; xi < 200; xi++) {

                someLabel:
                for (yi = 0; yi < 100; yi++) {

                    if (values2[xi][yi] != -1) {

                        for (int i = 0; i < 360; i = i + 60) {

                            float degInRad = i * DEG2RAD;

                            if (xi % 2 == 0) {
                                if ((Math.cos(degInRad) * pixel / 2 + xi * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel + pixel / 2) >= 600) {
                                    continue someLabel;
                                }
                            } else {
                                if ((Math.cos(degInRad) * pixel / 2 + (xi) * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + (yi + 0.5) * pixel + pixel / 2) >= 600) {
                                    continue someLabel;
                                }
                            }
                        }

                        if (xi % 2 == 1) {

                            if (yi - 1 >= 0) {
                                if (values2[xi][yi - 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (yi + 1 < 100) {
                                if (values2[xi][yi + 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi + 1 < 200) {
                                if (values2[xi + 1][yi] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi + 1 < 200 && yi + 1 < 100) {
                                if (values2[xi + 1][yi + 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi - 1 >= 0) {
                                if (values2[xi - 1][yi] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi - 1 >= 0 && yi + 1 < 100) {
                                if (values2[xi - 1][yi + 1] == -1) {
                                    tileValue++;
                                }
                            }

                        } else {
                            if (yi - 1 >= 0) {
                                if (values2[xi][yi - 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (yi + 1 < 100) {
                                if (values2[xi][yi + 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi + 1 < 200 && yi - 1 >= 0) {
                                if (values2[xi + 1][yi - 1] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi + 1 < 200) {
                                if (values2[xi + 1][yi] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi - 1 >= 0) {
                                if (values2[xi - 1][yi] == -1) {
                                    tileValue++;
                                }
                            }
                            if (xi - 1 >= 0 && yi - 1 >= 0) {
                                if (values2[xi - 1][yi - 1] == -1) {
                                    tileValue++;
                                }
                            }
                        }

                        values2[xi][yi] = tileValue;
                        tileValue = 0;
                    }

                }

            }
        }
    }

    private static void mouseClick() {
        try {
            Mouse.setGrabbed(false);
            Mouse.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        double xMouse = Mouse.getX();
        double yMouse = (600 - Mouse.getY());

        double xCentre;
        double yCentre;

        if (Mouse.isButtonDown(0) && !leftClick) {

            for (int xi = 0; xi < (1600 / pixel); xi++) {

                for (int yi = 0; yi < (1200 / pixel); yi++) {

                    if (vertexUpwards) {
                        if (yi % 2 == 0) {
                            xCentre = xi * pixel + pixel / 2;
                            yCentre = menu * (yi * pixel * 0.86 + pixel / 2);
                        } else {
                            xCentre = (xi + 0.5) * pixel + pixel / 2;
                            yCentre = menu * (yi * pixel * 0.86 + pixel / 2);
                        }

                        double distance = Math.sqrt(Math.pow(xMouse - xCentre, 2) + Math.pow(yMouse - yCentre, 2));

                        if (distance < pixel / 2 * menu) {
                            selectedHexagons[xi][yi] = 1;
                        }
                    } else {
                        if (xi % 2 == 0) {
                            xCentre = xi * pixel * 0.86 + pixel / 2;
                            yCentre = menu * (yi * pixel + pixel / 2);
                        } else {
                            xCentre = xi * pixel * 0.86 + pixel / 2;
                            yCentre = menu * ((yi + 0.5) * pixel + pixel / 2);
                        }

                        double distance = Math.sqrt(Math.pow(xMouse - xCentre, 2) + Math.pow(yMouse - yCentre, 2));

                        if (distance < pixel / 2 * menu) {
                            selectedHexagons[xi][yi] = 1;
                        }

                    }
                }
            }
            leftClick = true;
            if (timerStart == 1) {
                timerStart = 2;
                initialTime = System.nanoTime() / 1000000000;
            }

        }

        if (Mouse.isButtonDown(1) && !rightClick) {

            for (int xi = 0; xi < (1600 / pixel); xi++) {

                for (int yi = 0; yi < (1200 / pixel); yi++) {

                    if (vertexUpwards) {
                        if (yi % 2 == 0) {
                            xCentre = xi * pixel + pixel / 2;
                            yCentre = menu * (yi * pixel * 0.86 + pixel / 2);
                        } else {
                            xCentre = (xi + 0.5) * pixel + pixel / 2;
                            yCentre = menu * (yi * pixel * 0.86 + pixel / 2);
                        }

                        double distance = Math.sqrt(Math.pow(xMouse - xCentre, 2) + Math.pow(yMouse - yCentre, 2));

                        if (distance < pixel / 2 * menu) {
                            if (selectedHexagons[xi][yi] == 0) {
                                selectedHexagons[xi][yi] = 2;
                            } else if (selectedHexagons[xi][yi] == 2) {
                                selectedHexagons[xi][yi] = 0;
                            }
                        }
                    } else {
                        if (xi % 2 == 0) {
                            xCentre = xi * pixel * 0.86 + pixel / 2;
                            yCentre = menu * (yi * pixel + pixel / 2);
                        } else {
                            xCentre = xi * pixel * 0.86 + pixel / 2;
                            yCentre = menu * ((yi + 0.5) * pixel + pixel / 2);
                        }

                        double distance = Math.sqrt(Math.pow(xMouse - xCentre, 2) + Math.pow(yMouse - yCentre, 2));

                        if (distance < pixel / 2 * menu) {
                            if (selectedHexagons[xi][yi] == 0) {
                                selectedHexagons[xi][yi] = 2;
                            } else if (selectedHexagons[xi][yi] == 2) {
                                selectedHexagons[xi][yi] = 0;
                            }
                        }

                    }
                }
            }
            rightClick = true;
        }

        if (!Mouse.isButtonDown(0)) {
            leftClick = false;
        }
        if (!Mouse.isButtonDown(1)) {
            rightClick = false;
        }

    }

    private static void keyboardInput() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_E) {
                    vertexUpwards = !vertexUpwards;
                    if (vertexUpwards) {
                        values1 = new int[200][100];
                    } else {
                        values2 = new int[200][100];
                    }
                    if (covered) {
                        selectedHexagons = new int[200][100];
                        timerStart = 1;
                    } else {
                        uncover();
                        timerStart = 0;
                    }
                    layMines();
                } else if (Keyboard.getEventKey() == Keyboard.KEY_SPACE) {
                    if (menu == 1) {
                        menu = 0.95;
                    } else {
                        menu = 1;
                    }

                } else if (Keyboard.getEventKey() == Keyboard.KEY_Q) { //cover random
                    selectedHexagons = new int[200][100];
                    values1 = new int[200][100];
                    values2 = new int[200][100];
                    layMines();
                    covered = true;
                    timerStart = 1;
                } else if (Keyboard.getEventKey() == Keyboard.KEY_W) { //cover no random
                    selectedHexagons = new int[200][100];
                    covered = true;
                    timerStart = 1;
                } else if (Keyboard.getEventKey() == Keyboard.KEY_A) { //uncover random
                    uncover();
                    values1 = new int[200][100];
                    values2 = new int[200][100];
                    layMines();
                    covered = false;
                    timerStart = 0;
                } else if (Keyboard.getEventKey() == Keyboard.KEY_S) { //uncover no random
                    uncover();
                    covered = false;
                    timerStart = 0;
                }
            }
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            if (mineNumber >= 1) {
                mineNumber = mineNumber - 10;
                if (mineNumber < 0) {
                    mineNumber = 0;
                }
                values1 = new int[200][100];
                values2 = new int[200][100];
                layMines();
                if (covered) {
                    selectedHexagons = new int[200][100];
                    timerStart = 1;
                } else {
                    uncover();
                    timerStart = 0;
                }
            }

        } else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            mineNumber = mineNumber + 10;
            values1 = new int[200][100];
            values2 = new int[200][100];
            layMines();
            if (covered) {
                timerStart = 1;
                selectedHexagons = new int[200][100];
            } else {
                uncover();
                timerStart = 0;
            }

        } else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            pixel++;
            values1 = new int[200][100];
            values2 = new int[200][100];
            layMines();
            if (covered) {
                selectedHexagons = new int[200][100];
                timerStart = 1;
            } else {
                uncover();
                timerStart = 0;
            }

        } else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            if (pixel > 10) {
                pixel--;
                values1 = new int[200][100];
                values2 = new int[200][100];
                layMines();
                if (covered) {
                    selectedHexagons = new int[200][100];
                    timerStart = 1;
                } else {
                    uncover();
                    timerStart = 0;
                }
            }

        }
    }

    private static void flood() {

        int xi;
        int yi;
        float DEG2RAD = (float) (3.14159 / 180);

        int m = mineNumber;

        if (vertexUpwards) {

            for (xi = 0; xi < 200; xi++) {

                someLabel:
                for (yi = 0; yi < 100; yi++) {

                    if (values1[xi][yi] != -1) {

                        for (int i = 90; i < 450; i = i + 60) {

                            float degInRad = i * DEG2RAD;

                            if (yi % 2 == 0) {
                                if ((Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2) >= 600 || (Math.cos(degInRad) * pixel / 2 + xi * pixel + pixel / 2) >= 800) {
                                    continue someLabel;
                                }
                            } else {
                                if ((Math.sin(degInRad) * pixel / 2 + (yi) * pixel * 0.86 + pixel / 2) >= 600 || (Math.cos(degInRad) * pixel / 2 + (xi + 0.5) * pixel + pixel / 2) >= 800) {
                                    continue someLabel;
                                }
                            }
                        }

                        if (selectedHexagons[xi][yi] == 1 && values1[xi][yi] == 0) {

                            if (yi % 2 == 1) {

                                if (xi + 1 < 200) {
                                    selectedHexagons[xi + 1][yi] = 1;
                                }
                                if (xi + 1 < 200 && yi - 1 >= 0) {
                                    selectedHexagons[xi + 1][yi - 1] = 1;
                                }
                                if (xi + 1 < 200 && yi + 1 < 100) {
                                    selectedHexagons[xi + 1][yi + 1] = 1;
                                }
                                if (yi + 1 < 100) {
                                    selectedHexagons[xi][yi + 1] = 1;
                                }
                                if (xi - 1 >= 0) {
                                    selectedHexagons[xi - 1][yi] = 1;
                                }
                                if (yi - 1 >= 0) {
                                    selectedHexagons[xi][yi - 1] = 1;
                                }

                            } else {
                                if (yi - 1 >= 0) {
                                    selectedHexagons[xi][yi - 1] = 1;
                                }
                                if (xi + 1 < 200) {
                                    selectedHexagons[xi + 1][yi] = 1;
                                }
                                if (yi + 1 < 100) {
                                    selectedHexagons[xi][yi + 1] = 1;
                                }
                                if (xi - 1 >= 0 && yi + 1 < 100) {
                                    selectedHexagons[xi - 1][yi + 1] = 1;
                                }
                                if (xi - 1 >= 0) {
                                    selectedHexagons[xi - 1][yi] = 1;
                                }
                                if (xi - 1 >= 0 && yi - 1 >= 0) {
                                    selectedHexagons[xi - 1][yi - 1] = 1;
                                }
                            }
                        }
                    }

                }
            }
        } else {

            for (xi = 0; xi < 200; xi++) {

                someLabel:
                for (yi = 0; yi < 100; yi++) {

                    if (values2[xi][yi] != -1) {

                        for (int i = 0; i < 360; i = i + 60) {

                            float degInRad = i * DEG2RAD;

                            if (xi % 2 == 0) {
                                if ((Math.cos(degInRad) * pixel / 2 + xi * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel + pixel / 2) >= 600) {
                                    continue someLabel;
                                }
                            } else {
                                if ((Math.cos(degInRad) * pixel / 2 + (xi) * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + (yi + 0.5) * pixel + pixel / 2) >= 600) {
                                    continue someLabel;
                                }
                            }
                        }

                        if (selectedHexagons[xi][yi] == 1 && values2[xi][yi] == 0) {

                            if (xi % 2 == 1) {

                                if (yi - 1 >= 0) {
                                    selectedHexagons[xi][yi - 1] = 1;
                                }
                                if (yi + 1 < 100) {
                                    selectedHexagons[xi][yi + 1] = 1;
                                }
                                if (xi + 1 < 200) {
                                    selectedHexagons[xi + 1][yi] = 1;
                                }
                                if (xi + 1 < 200 && yi + 1 < 100) {
                                    selectedHexagons[xi + 1][yi + 1] = 1;
                                }
                                if (xi - 1 >= 0) {
                                    selectedHexagons[xi - 1][yi] = 1;
                                }
                                if (xi - 1 >= 0 && yi + 1 < 100) {
                                    selectedHexagons[xi - 1][yi + 1] = 1;
                                }

                            } else {
                                if (yi - 1 >= 0) {
                                    selectedHexagons[xi][yi - 1] = 1;
                                }
                                if (yi + 1 < 100) {
                                    selectedHexagons[xi][yi + 1] = 1;
                                }
                                if (xi + 1 < 200 && yi - 1 >= 0) {
                                    selectedHexagons[xi + 1][yi - 1] = 1;
                                }
                                if (xi + 1 < 200) {
                                    selectedHexagons[xi + 1][yi] = 1;
                                }
                                if (xi - 1 >= 0) {
                                    selectedHexagons[xi - 1][yi] = 1;
                                }
                                if (xi - 1 >= 0 && yi - 1 >= 0) {
                                    selectedHexagons[xi - 1][yi - 1] = 1;
                                }
                            }

                        }
                    }

                }

            }
        }
    }

    private static void draw() {

        for (int xi = 0; xi < 200; xi++) {

            someLabel:
            for (int yi = 0; yi < 100; yi++) {

                float DEG2RAD = (float) (3.14159 / 180);

                if (vertexUpwards) {

                    for (int i = 90; i < 450; i = i + 60) {

                        float degInRad = i * DEG2RAD;

                        if (yi % 2 == 0) {
                            if ((Math.cos(degInRad) * pixel / 2 + xi * pixel + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2) >= 600) {
                                continue someLabel;
                            }
                        } else {
                            if ((Math.cos(degInRad) * pixel / 2 + (xi + 0.5) * pixel + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2) >= 600) {
                                continue someLabel;
                            }
                        }
                    }

                    if (selectedHexagons[xi][yi] == 0) {
                        Colour(-2);
                    } else if (selectedHexagons[xi][yi] == 2) {
                        Colour(-3);
                    } else {
                        Colour(values1[xi][yi]);
                    }

                    GL11.glBegin(GL11.GL_POLYGON);

                    for (int i = 90; i < 450; i = i + 60) {

                        float degInRad = i * DEG2RAD;

                        if (yi % 2 == 0) {
                            GL11.glVertex2f((float) (Math.cos(degInRad) * pixel / 2 + xi * pixel + pixel / 2), (float) (menu * (Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2)));
                        } else {
                            GL11.glVertex2f((float) (Math.cos(degInRad) * pixel / 2 + (xi + 0.5) * pixel + pixel / 2), (float) (menu * (Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2)));
                        }

                    }
                    GL11.glEnd();

                } else {

                    for (int i = 0; i < 360; i = i + 60) {

                        float degInRad = i * DEG2RAD;

                        if (xi % 2 == 0) {
                            if ((Math.cos(degInRad) * pixel / 2 + xi * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel + pixel / 2) >= 600) {
                                continue someLabel;
                            }
                        } else {
                            if ((Math.cos(degInRad) * pixel / 2 + (xi) * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + (yi + 0.5) * pixel + pixel / 2) >= 600) {
                                continue someLabel;
                            }
                        }
                    }

                    if (selectedHexagons[xi][yi] == 0) {
                        Colour(-2);
                    } else if (selectedHexagons[xi][yi] == 2) {
                        Colour(-3);
                    } else {
                        Colour(values2[xi][yi]);
                    }

                    GL11.glBegin(GL11.GL_POLYGON);

                    for (int i = 0; i < 360; i = i + 60) {

                        float degInRad = i * DEG2RAD;

                        if (xi % 2 == 0) {
                            GL11.glVertex2f((float) (Math.cos(degInRad) * pixel / 2 + xi * pixel * 0.86 + pixel / 2), (float) (menu * (Math.sin(degInRad) * pixel / 2 + yi * pixel + pixel / 2)));
                        } else {
                            GL11.glVertex2f((float) (Math.cos(degInRad) * pixel / 2 + (xi) * pixel * 0.86 + pixel / 2), (float) (menu * (Math.sin(degInRad) * pixel / 2 + (yi + 0.5) * pixel + pixel / 2)));
                        }

                    }

                    GL11.glEnd();
                }
            }

        }

    }

    public static void Colour(int index) {

        if (index == -3) {
            GL11.glColor3ub((byte) 255, (byte) 255, (byte) 0); //yellow;
        }
        if (index == -2) {
            GL11.glColor3ub((byte) 55, (byte) 55, (byte) 55); //gray
        }
        if (index == -1) {
            GL11.glColor3ub((byte) 255, (byte) 255, (byte) 255); //white
        }
        if (index == 0) {

            GL11.glColor3ub((byte) 0, (byte) 0, (byte) 0);
        }
        if (index == 1) {

            GL11.glColor3ub((byte) 139, (byte) 69, (byte) 19);

        }
        if (index == 2) {
            GL11.glColor3ub((byte) 0, (byte) 128, (byte) 0);

        }
        if (index == 3) {
            GL11.glColor3ub((byte) 0, (byte) 0, (byte) 255);

        }
        if (index == 4) {
            GL11.glColor3ub((byte) 128, (byte) 0, (byte) 128);

        }
        if (index == 5) {

            GL11.glColor3ub((byte) 255, (byte) 0, (byte) 0);
        }
        if (index == 6) {

            GL11.glColor3ub((byte) 0, (byte) 255, (byte) 255);
        }
    }

    public static void uncover() {
        for (int xi = 0; xi < 200; xi++) {

            for (int yi = 0; yi < 100; yi++) {
                selectedHexagons[xi][yi] = 1;

            }
        }
    }

    public static void tileCount() {

        float DEG2RAD = (float) (3.14159 / 180);

        tileNumber = 0;
        flagNumber = 0;

        if (vertexUpwards) {
            for (int xi = 0; xi < 200; xi++) {

                someLabel:
                for (int yi = 0; yi < 100; yi++) {

                    if (selectedHexagons[xi][yi] == 2) {
                        flagNumber++;
                    }

                    for (int i = 90; i < 450; i = i + 60) {

                        float degInRad = i * DEG2RAD;

                        if (yi % 2 == 0) {
                            if ((Math.cos(degInRad) * pixel / 2 + xi * pixel + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2) >= 600) {
                                continue someLabel;
                            }
                        } else {
                            if ((Math.cos(degInRad) * pixel / 2 + (xi + 0.5) * pixel + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel * 0.86 + pixel / 2) >= 600) {
                                continue someLabel;
                            }
                        }
                    }

                    tileNumber++;
                }

            }
            if (tileNumber < mineNumber) {
                mineNumber = tileNumber;
            }

        } else {

            for (int xi = 0; xi < 200; xi++) {

                someLabel:
                for (int yi = 0; yi < 100; yi++) {

                    if (selectedHexagons[xi][yi] == 2) {
                        flagNumber++;
                    }

                    for (int i = 0; i < 360; i = i + 60) {

                        float degInRad = i * DEG2RAD;

                        if (xi % 2 == 0) {
                            if ((Math.cos(degInRad) * pixel / 2 + xi * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + yi * pixel + pixel / 2) >= 600) {
                                continue someLabel;
                            }
                        } else {
                            if ((Math.cos(degInRad) * pixel / 2 + (xi) * pixel * 0.86 + pixel / 2) >= 800 || (Math.sin(degInRad) * pixel / 2 + (yi + 0.5) * pixel + pixel / 2) >= 600) {
                                continue someLabel;
                            }
                        }
                    }
                    tileNumber++;
                }

            }
            if (tileNumber < mineNumber) {
                mineNumber = tileNumber;
            }

        }
    }

    public static void checkWinLose() {

        int correct = 0;

        if (vertexUpwards) {

            for (int xi = 0; xi < 200; xi++) {

                for (int yi = 0; yi < 100; yi++) {

                    if (covered && selectedHexagons[xi][yi] == 1 && (values1[xi][yi] == -1)) {
                        uncover();
                        GL11.glColor3ub((byte) 50, (byte) 0, (byte) 0);
                        GL11.glBegin(GL11.GL_QUADS);
                        GL11.glVertex2f(0, 0);
                        GL11.glVertex2f(800, 0);
                        GL11.glVertex2f(800, 600);
                        GL11.glVertex2f(0, 600);
                        GL11.glEnd();

                        if (timerStart == 2) {
                            savedTime = (int) (time - initialTime);
                        }
                        timerStart = 3;
                    }

                    if (flagNumber == mineNumber && selectedHexagons[xi][yi] == 2 && values1[xi][yi] == -1) {
                        correct++;
                    }

                }
            }
            if (mineNumber != 0 && correct == mineNumber) {

                GL11.glColor3ub((byte) 0, (byte) 50, (byte) 0);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(0, 0);
                GL11.glVertex2f(800, 0);
                GL11.glVertex2f(800, 600);
                GL11.glVertex2f(0, 600);
                GL11.glEnd();

                if (timerStart == 2) {
                    savedTime = (int) (time - initialTime);
                }
                timerStart = 3;
            }
        } else {
            for (int xi = 0; xi < 200; xi++) {

                for (int yi = 0; yi < 100; yi++) {

                    if (covered && selectedHexagons[xi][yi] == 1 && values2[xi][yi] == -1) {
                        uncover();
                        GL11.glColor3ub((byte) 50, (byte) 0, (byte) 0);
                        GL11.glBegin(GL11.GL_QUADS);
                        GL11.glVertex2f(0, 0);
                        GL11.glVertex2f(800, 0);
                        GL11.glVertex2f(800, 600);
                        GL11.glVertex2f(0, 600);
                        GL11.glEnd();

                        if (timerStart == 2) {
                            savedTime = (int) (time - initialTime);
                        }
                        timerStart = 3;
                    }

                    if (flagNumber == mineNumber && selectedHexagons[xi][yi] == 2 && values2[xi][yi] == -1) {
                        correct++;
                    }

                }
            }
            if (mineNumber != 0 && correct == mineNumber) {

                GL11.glColor3ub((byte) 0, (byte) 50, (byte) 0);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(0, 0);
                GL11.glVertex2f(800, 0);
                GL11.glVertex2f(800, 600);
                GL11.glVertex2f(0, 600);
                GL11.glEnd();

                if (timerStart == 2) {
                    savedTime = (int) (time - initialTime);
                }
                timerStart = 3;
            }

        }
    }

    public static void drawMenu(TrueTypeFont trueTypeBylineFont) {
        if (menu != 1) {
            GL11.glEnable(GL11.GL_TEXTURE_2D); //Enables textures.

            trueTypeBylineFont.drawString((800 - trueTypeBylineFont.getWidth("Tiles: " + tileNumber)) / 5, 570, "Tiles: " + tileNumber, Color.white);

            trueTypeBylineFont.drawString((800 - trueTypeBylineFont.getWidth("Mines: " + mineNumber)) * 2 / 5, 570, "Mines: " + mineNumber, Color.white);

            trueTypeBylineFont.drawString((800 - trueTypeBylineFont.getWidth("Flags: " + flagNumber)) * 3 / 5, 570, "Flags: " + flagNumber, Color.white);

            if (timerStart == 3) {
                trueTypeBylineFont.drawString((800 - trueTypeBylineFont.getWidth("Time: " + savedTime)) * 4 / 5, 570, "Time: " + savedTime, Color.white);
            } else if (timerStart == 2) {
                trueTypeBylineFont.drawString((800 - trueTypeBylineFont.getWidth("Time: " + (int) (time - initialTime))) * 4 / 5, 570, "Time: " + (int) (time - initialTime), Color.white);
            } else {
                trueTypeBylineFont.drawString((800 - trueTypeBylineFont.getWidth("Time: 0")) * 4 / 5, 570, "Time: 0", Color.white);
            }

            GL11.glDisable(GL11.GL_TEXTURE_2D); //Disables textures.
        }
    }

    public static void titleScreen(TrueTypeFont trueTypeTitleFont, TrueTypeFont trueTypeBylineFont) {
        GL11.glEnable(GL11.GL_TEXTURE_2D); //Enables textures.

        trueTypeTitleFont.drawString((800 - trueTypeTitleFont.getWidth("Hexagonal Minesweeper")) / 2, (600 - trueTypeTitleFont.getHeight("Hexagonal Minesweeper")) / 2, "Hexagonal Minesweeper", Color.white);

        trueTypeBylineFont.drawString(650, 570, "By Carson Craig", Color.white);

        GL11.glDisable(GL11.GL_TEXTURE_2D); //Disables textures.    
    }

}
