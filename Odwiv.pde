float[] a; // Stores height values of each surface pixel
ArrayList <Wave> waves;

void setup() {

  size(1000, 500);
  a = new float[width];

  waves = new ArrayList <Wave>();

  stroke(0);
  strokeWeight(1);
  rectMode(CENTER);
}

void draw() {
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


void crests() {
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
void ripple() {
  for (Wave w : waves) {
    w.update();
  }
}

void paintSea() {
  stroke(160, 225, 245);

  for (int i = 0; i < width; i++) {
    line(i, a[i] - 1 + height/2, i, height);
  }
}


void paintDots() {

  PVector prev = new PVector(0, 0);
  for (int i = 0; i < width; i++) {
    if (i > 0)
      line(i, a[i] + height / 2, prev.x, prev.y);
    prev = new PVector(i, a[i] + height / 2);
  }
}

void paintSq() {
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


void cAll() {
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

void constrain(int i) {

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

void mousePressed() {
  
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

void mouseMoved() {
  act();
}

void mouseDragged() {
  act();
}

// update the wave's preferences
void act() {
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

void mouseReleased() {
  if (but) {
    but = false;
    return;
  }
}


void velBar() {
  Wave cur = waves.get(waves.size() - 1);
  
  // draw the bar below the wave if it goes up and
  // above if it goes down
  float sy = (cur.peak < 0) ?  25: -25;

  int x = (int) cur.pos;

  // constrain velocity
  if (abs(vel) > 300) vel = 300 * Math.signum(vel);

  color green = color(50, 150, 0);
  color red = color(237, 33, 8);  
  
  float u = vel / 3;
  
  // Make the gradien happen!
    
  // NOTE: three pixels of "vel" = one pixel of the bar
  if (vel > 0) {
    for (int i = x; i <= x + u; i++) {
      float inter = map(i, x, x + 100, 0, 1);
      color c = lerpColor(green, red, inter);
      stroke(c);
      line(i, height/2 + sy, i, height/2 + sy+20);
    }
  }

  if (vel < 0) {
    for (int i = x; i >= x + u; i--) {
      float inter = map(i, x, x - 100, 0, 1);
      color c = lerpColor(green, red, inter);
      stroke(c);
      line(i, height/2 + sy, i, height/2 + sy+20);
    }
  }
}
