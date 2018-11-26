const int xPin = 0;
const int yPin = 1;
const int zPin = 2;

int minVal = 265;
int maxVal = 402;

double x;
double y;
double z;
int prev_val;
unsigned long last_millis;
unsigned long count = 0; // 1보에 2씩 증가 걸음 수는 count/2임.

void setup()
{
  Serial.begin(9600); 
}


void loop()
{
  int xRead = analogRead(xPin);
  int yRead = analogRead(yPin);
  int zRead = analogRead(zPin);

  int xAng = map(xRead, minVal, maxVal, -90, 90);
  int yAng = map(yRead, minVal, maxVal, -90, 90);
  int zAng = map(zRead, minVal, maxVal, -90, 90);

  x = RAD_TO_DEG * (atan2(-yAng, -zAng) + PI);
  y = RAD_TO_DEG * (atan2(-xAng, -zAng) + PI);
  z = RAD_TO_DEG * (atan2(-yAng, -xAng) + PI);
  
    x = x / 10;
    y = y / 10;
    z = z / 10;
 
  int val = x * x + y * y + z * z;
      if ( ( val > 500 ) && ( prev_val <= 500 ) )
    {
        if ( ( millis() - last_millis ) > 300 ) // 0.3초 미만 간격으로 걸음이 추가되는 경우는 실제 걸음이 아니라 센서의 흔들림 및 노이즈일 가능성이 많음.
        {
            count++;
            last_millis = millis();
        }
    }
    prev_val = val;
    //        Serial.println(val);
    Serial.println(count/2);

    
  Serial.print("x: ");
  Serial.print(x);
  Serial.print(" | y: ");
  Serial.print(y);
  Serial.print(" | z: ");
  Serial.println(z);

  delay(300);
}
