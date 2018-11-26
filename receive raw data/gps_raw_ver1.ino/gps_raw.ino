
void setup() {
  // wait for hardware serial to appear
  //while (!Serial);

  // make this baud rate fast enough to we aren't waiting on it
  // Serial.begin(115200); 
  // Serial 통신중 9600 baud rate 만으로도 통신 가능 - 테스팅 완료

  // 9600 baud is the default rate for the Ultimate GPS
  Serial1.begin(9600);
}
     
void loop() {/*
  if (Serial.available()) {
    char c = Serial.read();
    Serial1.write(c);
  }*/
  if (Serial1.available()) {
    char c = Serial1.read();
    Serial.write(c);
  }
}
