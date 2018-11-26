#include <Servo.h> 

Servo myservo; 
const int servoPin = 9; // servo pin

void setup() {
  myservo.attach(servoPin); //서보 핀으로 GPIO 용 pin 9 지정
}

void loop() {
  myservo.write(0);  //Move angle 0          
  delay(1000);       //wait 1 second                 
  myservo.write(90); //Move angle 90           
  delay(1000);       //wait 1 second  
  myservo.write(180);///Move angle 180           
  delay(1000);       //wait 1 second             
}
