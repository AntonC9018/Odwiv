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

  void update() {
    pos += vel;

    // reflect off a wall
    if (!b && (pos > width || pos < 0)) { 
      vel *= -1;
      b = true;
    } else 
    b = false;
  }

  // compute this wave's contriution
  float[] comp() {

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
