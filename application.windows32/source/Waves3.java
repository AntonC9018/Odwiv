import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Waves3 extends PApplet {

float[] a; // Stores height values of each surface pixel
ArrayList <Wave> waves;

public void setup() {
  

  
  a = new float[width];  
  
  surface.setTitle("Odwiv");

  waves = new ArrayList <Wave>();

  stroke(0);
  strokeWeight(1);
  rectMode(CENTER);
}

public void draw() {
  background(255);

  // Red button deletes all waves
  fill(255, 0, 0);
  rect(15, 15, 30, 30);  

  // Update the position of each wave
  ripple();

  // Calculate the height for each surface pixel
  crests();

  // Constrain waves from getting off the top and bottom
  // by toppling the ones that do this (see method below 
  // for details
  //cAll();

  // Give water a nice color
  paintSea();

  stroke(0);

  // Draw buoyant squares
  paintSq();

  // Draw the contour of the surface of the water
  paintDots();

  // Draw the velocity bar (see mouse methods)
  if (sphase) 
    velBar();
}


public void crests() {
  // create new, plain water surface
  float[] t = new float[width];

  // add up contribution of each wave
  // (superposition principle)
  for (Wave w : waves) {
    float[] t2 = w.comp();
    for (int i = 0; i < width; i++)
      t[i] += t2[i];
  }
  // update surface
  a = t;
}

// Move each wave
public void ripple() {
  for (Wave w : waves) {
    w.update();
  }
}

public void paintSea() {
  stroke(160, 225, 245);

  for (int i = 0; i < width; i++) {
    line(i, a[i] - 1 + height/2, i, height);
  }
}


public void paintDots() {

  PVector prev = new PVector(0, 0);
  for (int i = 0; i < width; i++) {
    if (i > 0)
      line(i, a[i] + height / 2, prev.x, prev.y);
    prev = new PVector(i, a[i] + height / 2);
  }
}

public void paintSq() {
  int u = width / 100;  
  float q = 0;
  
  // enable this for squares to be over the surface
  // q = -u/2;
  
  noFill();
    
  // enable this for squares to be not transparent in the water
  // fill(255);
  
  
  for (int i = 0; i < 100; i++) {
    float av = 0;
    for (int j = u * (i); j < u*(i+1); j++) {
      av += a[j];
    }
    av /= u;

    rect(i*u + u/2, av + height / 2 + q, u, u);
  }
}


public void cAll() {
  float sum = 0;

  for (int i = 0; i < a.length; i++) {
    sum += a[i];
  }
  
  // if there's too much mass in the "aquarium", do nothing
  if (abs(sum) > width * height/2) return;

  // try to topple each wave
  for (int i = 0; i < a.length; i++) {
    constrain(i);
  }
}

public void constrain(int i) {

  // return if no topple required
  if (abs(a[i]) < height / 2) return;

  // calculate the excess mass (which goes off top or bottom)
  float excess;  
  excess = (a[i] < 0) ? a[i] + height/2 : a[i] - height/2;  
  
  // decrement the valuse of the pixel as if it topples the
  // excess mass to adjacent pixels
  a[i] -= excess;
  
  // if two pixels have stuck in a loop, toppling half of their
  // mass one to other, at some point it must be stopped
  // if the excess mass is too low, do not consider toppling at all
  if (abs(excess) < 3) return;

  // left-most pixel case
  if (i == 0) {
    a[1] += excess;
    return;
    // we don't need to topple the pixel right after this one
    // because it will be toppled anyway. Its turn is next.
  }

  // right-most pixel case
  if (i == width - 1) {
    a[width - 2] += excess;
    constrain(width - 2);
    return;
  }

  // topple to the right if the pixel is in the left part of the screen
  // and the pixel right before it is already full
  if (i <= width/2 && abs(a[i-1] + excess/2) >= height / 2) {
    a[i+1] += excess;
    //constrain(i+1);
    return;
  }
  
  // topple to the left if the pixel is in the right part of the screen
  // and the pixel right after it is already full
  if (i > width/2 && abs(a[i+1] + excess/2) >= height / 2) {
    a[i-1] += excess;
    // here we need to topple the pixel before this 
    // because its turn has gone
    constrain(i-1);
    return;
  }
  
  // topple half to the right and half to the left
  a[i-1] += excess/2;
  a[i+1] += excess/2;

  constrain(i-1);
  //constrain(i+1);
}

// button has been pressed
boolean but;

// first phase (wave's height and length adjustment)
boolean fphase = false;

// second phase (wave's velocity adjustment)
boolean sphase = false;

// x coordinate of the future wave
float start;

// velocity of the future wave
float vel;

public void mousePressed() {
  
  // delete waves if the red button is pressed
  if (mouseX < 30 && mouseY < 30) {
    waves.clear(); 
    but = true;
    fphase = false;
    sphase = false;
    return;
  }
  
  // first click, no phase
  if (!fphase && !sphase) {
    start = mouseX;
    // enter first phase
    fphase = true;
    return;
  }
  
  // clicked in first phase
  if (fphase) {
    // enter second phase
    sphase = true;
    fphase = false;
    vel = mouseX - start;
    return;
  }

  // clicked in second phase, create a wave
  if (sphase) {

    // get the characteristics of the last wave
    // which is always idle
    Wave u = waves.get(waves.size() - 1);
    
    // destroy it.
    waves.remove(waves.size() - 1);
    
    // constrain velocity to 10 pixels per frame
    if (abs(vel) > 300) vel = 300 * Math.signum(vel);

    // create the wave
    waves.add(new Wave(start, map(vel, 0, 300, 0, 10), u.len, u.peak));

    // enter null phase
    sphase = false;
    return;
  }
}

public void mouseMoved() {
  act();
}

public void mouseDragged() {
  act();
}

// update the wave's preferences
public void act() {
  if (but) return;
  
  // adjust height while in the first phase
  if (fphase) {
    float h = mouseY - height / 2;
    
    if (waves.size() > 0 && waves.get(waves.size() - 1).vel == 0)
      waves.remove(waves.size() - 1);

    waves.add(new Wave(start, 0, abs(ceil(start - mouseX)), h));
  }
  
  // adjust velocity while in the second phase
  if (sphase) {
    vel = mouseX - start;
    println(vel);
  }
}

public void mouseReleased() {
  if (but) {
    but = false;
    return;
  }
}


public void velBar() {
  Wave cur = waves.get(waves.size() - 1);
  
  // draw the bar below the wave if it goes up and
  // above if it goes down
  float sy = (cur.peak < 0) ?  25: -25;

  int x = (int) cur.pos;

  // constrain velocity
  if (abs(vel) > 300) vel = 300 * Math.signum(vel);

  int green = color(50, 150, 0);
  int red = color(237, 33, 8);  
  
  float u = vel / 3;
  
  // Make the gradien happen!
    
  // NOTE: three pixels of "vel" = one pixel of the bar
  if (vel > 0) {
    for (int i = x; i <= x + u; i++) {
      float inter = map(i, x, x + 100, 0, 1);
      int c = lerpColor(green, red, inter);
      stroke(c);
      line(i, height/2 + sy, i, height/2 + sy+20);
    }
  }

  if (vel < 0) {
    for (int i = x; i >= x + u; i--) {
      float inter = map(i, x, x - 100, 0, 1);
      int c = lerpColor(green, red, inter);
      stroke(c);
      line(i, height/2 + sy, i, height/2 + sy+20);
    }
  }
}
class Wave {

  // length of wave (in pixels)
  int len;

  // height of wave
  float peak;

  float vel;
  float pos;


  Wave(float p, float v, int l, float e) {    
    len = l;
    peak = e;
    vel = v;
    pos = p;
  }

  // reflected (for it not to be stuck in the wall for whatever reason)
  boolean b = false;

  public void update() {
    pos += vel;

    // reflect off a wall
    if (!b && (pos > width || pos < 0)) { 
      vel *= -1;
      b = true;
    } else 
    b = false;
  }

  // compute this wave's contriution
  public float[] comp() {

    // create local map (the shape of the wave)
    float[] t = new float[len];
    float step = PI / len;

    for (int i = 0; i < len; i++) {
      t[i] = sin(step * i) * peak;
      
      // the highest point, or crest, is the "peak" value, which happens
      // when sine(x) is in its peak too, which will always be the
      // middle of the wave. The whole wave is PI radians, i.e.
      // the peak is always at PI/2 radians, when sine(x) = 1
      //
      // You could meddle around with the shape of the wave,
      // by adding some extra terms. 
      // 
      // Examples: (Some random functions)
      // t[i] = sin(step*i) * peak * abs(map(abs(i-len), 0, len, -1, 1));
      // t[i] = sin(step * i * 1 / (abs(map(i, 0, len, -10, 10)) + 1)) * peak;
      // t[i] = peak * (1 - (float)Math.pow(((abs(map(i, 0, len, -1, 1)))), 3));
      // t[i] = peak * (1 - (float)Math.pow(((abs(map(i, 0, len, -1, 1)))), 1.5));
    }

    // integrate the "local map" into a new surface array
    float[] temp = new float[width];
    int posM = (int)(pos - len/2);
    int c = 0;

    for (int i = 0; i < len; i++) {    
      
      // if it hits the wall, make it go the other way
      if (posM + i >= width) {               
        c--;
        temp[width + c] += t[i];
      } else if (posM + i < 0) {
        temp[- posM - i - 1] += t[i];
      } else {
        temp[posM + i] += t[i];
      }
    }

    return temp;
  }
}

  public void settings() {  size(1000, 500); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Waves3" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
