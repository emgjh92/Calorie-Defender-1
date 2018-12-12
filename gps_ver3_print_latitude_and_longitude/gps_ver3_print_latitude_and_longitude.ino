//#include <SoftwareSerial.h>
#include <TinyGPS.h>
 //https://github.com/mikalhart/TinyGPS/releases/tag/v13 ==> TinyGPS 라이브러리 다운로드 링크
// Create an instance of the TinyGPS object
TinyGPS gps;

//SoftwareSerial uart_gps(RXPIN, TXPIN);
#define uart_gps Serial1
// This is where you declare prototypes for the functions that will be 
// using the TinyGPS library.
void getgps(TinyGPS &gps);
 
// In the setup function, you need to initialize two serial ports; the 
// standard hardware serial port (Serial()) to communicate with your 
// terminal program an another serial port (NewSoftSerial()) for your 
// GPS.
void setup()
{
  // This is the serial rate for your terminal program. It must be this 
  // fast because we need to print everything before a new sentence 
  // comes in. If you slow it down, the messages might not be valid and 
  // you will likely get checksum errors.
  Serial.begin(115200);
  //Sets baud rate of your GPS
  uart_gps.begin(9600);

}
 
void loop()
{
  while(uart_gps.available())     // While there is data on the RX pin...
  {
      int c = uart_gps.read();    // load the data into a variable...
      if(gps.encode(c))      // if there is a new valid sentence...
      {
      //uart_gps.write(c);
        getgps(gps);         // then grab the data.
      }   
  }
}
 
// The getgps function will get and print the values we want.
void getgps(TinyGPS &gps)
{
  // To get all of the data into varialbes that you can use in your code, 
  // all you need to do is define variables and query the object for the 
  // data. To see the complete list of functions see keywords.txt file in 
  // the TinyGPS and NewSoftSerial libs.
  
  // Define the variables that will be used
  float latitude, longitude;
  // Then call this function
  gps.f_get_position(&latitude, &longitude);
  // You can now print variables latitude and longitude
  Serial.print("Lat/Long: "); 
  Serial.print(latitude,5); 
  Serial.print(","); 
  Serial.println(longitude,5);
  
  delay(100);
}

//
//참조: http://deneb21.tistory.com/331 [Do It Yourself!]
