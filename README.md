# Calorie Defender

Calorie Defender는 다이어트의 실패 원인 중 가장 큰 부분을 차지하는 식욕을 억제할 수 있도록 도와주고자 하는 카드 케이스로 사용자가 지정한 고깃집, 패스트푸드점, 디저트 가게 등의 장소에서 카드 케이스가 열리지 않도록 함으로써 카드를 사용할 수 없게 한다.

<p align="center"><img src="https://github.com/awakening95/etc/blob/master/Calorie%20Defender/Calorie%20Defender.gif?raw=true"></p>

**※ Calorie Defender는 전자부품연구원(KETI)에서 제공하는 오픈소스 IoT 플랫폼인 Mobius를 이용하여 제작되었다.**

## 목차

- [Overview](#overview)
- [Components](#components)
- [How to use](#how-to-use)
  - [1. Pinout](#1-pinout)

## Overview
Calorie Defender는 다음과 같이 구성되며 Calorie Defender를 만들기 위해 사용된 장치들에 대한 내용은 Components에서 확인할 수 있다.

<p align="center"><img src="https://github.com/awakening95/etc/blob/master/Calorie%20Defender/Calorie%20Defender%20Overview.png?raw=true"></p>

## Components
Calorie Defender의 장치는 다음과 같이 구성되며 각 장치의 Pinout은 How to use 의 1번에 설명되어 있다.

<p align="center"><img src="https://github.com/awakening95/etc/blob/master/Calorie%20Defender/Calorie%20Defender%20Components.png?raw=true"></p>

## How to use

### 1. Pinout

먼저 각 장치를 다음과 같이 연결한다.

<p align="center"><img src="https://github.com/awakening95/etc/blob/master/Calorie%20Defender/Calorie%20Defender%20Pinout.png?raw=true" width=500></p>

| **MPU-6050 (Gyro Sensor)** | **SG -90 (Servo Motor)** | **Adafruit Ultimate GPS Breakout:ada-746 (GPS Module)** |
| :--------: | :--------:            | :--------: |
| VCC - 3V   | Red( VCC ) - 3V or 5V | 3.3V - 3V  |	
| GND - GND  | Orange (PWM ) - 9~    | TX - RX    |
| SCL - SCL  | Brown(GND) - GND      | RX - TX    |
| SDA - SDA  |                       | GND - GND  |

**베터리의 경우 3.7V의 500mAh 리튬 폴리머 배터리를 사용하였으나 2.54mm pitch 를 지원하는 다른 베터리를 사용해도 무방하다.**

### 2. Arduino upload

nCube 폴더의 nCube.ino 파일을 열고 154 Line에 있는 AE_NAME을 자신이 원하는 AE_NAME으로 변경한다.

<p align="center"><img src="https://user-images.githubusercontent.com/39123255/50833834-d8364180-1395-11e9-82aa-2c9252492a95.png" width=500></p>

그리고 Adafruit feather m0 보드를 PC에 연결한 다음 툴탭에서 보드를 Adafruit Feather M0로 선택하고 포트를 Adafruit feather m0 보드가 연결된 포트로 선택한다.

<p align="center"><img src="https://user-images.githubusercontent.com/39123255/50834153-a5d91400-1396-11e9-9b73-3d3a9a325886.png" width=500></p>

그리고 업로드 버튼을 클릭한다.

<p align="center"><img src="https://user-images.githubusercontent.com/39123255/50834462-6959e800-1397-11e9-88c9-dc8192e5db2b.png" width=500></p>

### 3. WiFi 연결 및 서버의 데이터 저장 확인

업로드를 성공한 후 WiFi 목록을 보면 'wifi101 -' 다음에 Adafruit feather m0 보드의 무선 LAN 카드의 MAC 주소 뒤의 4자리로 구성된 SSID를 확인할 수 있다. 

<p align="center"><img src="https://user-images.githubusercontent.com/39123255/50834583-c0f85380-1397-11e9-8495-8f1b4932d3ac.png" width=500></p>

해당 WiFi로 연결을 한 후 http://wifi101.local/ 주소로 들어가면 다음과 같은 화면을 볼 수 있다. 그리고 Calorie Defender가 인터넷을 사용하여 Mobius Server에 데이터를 전송할 수 있게끔 사용할 공유기의 WiFi 이름(SSID)을 Network Name에 넣고 WiFi의 비밀번호를 Pass Phrase에 입력한 뒤 Connect 버튼을 누른다.

<p align="center"><img src="https://user-images.githubusercontent.com/39123255/50836538-c3a97780-139c-11e9-97b4-a8238a94a4e8.png" width=500></p>

성공적으로 공유기에 Calorie Defender가 연결되면 http://203.253.128.161:7575/#!/monitor에 들어가 /Mobius/'자신이 입력한 AE_NAME'을 Target Resource에 입력하고 Start 버튼을 누르면 다음과 같은 리소스 트리를 확인할 수 있다.(현재는 서비스를 이용할 수 없음)

<p align="center"><img src="https://user-images.githubusercontent.com/39123255/50836321-4d0c7a00-139c-11e9-8eb8-8612c01702f7.png" width=750></p>

<p align="center"><img src="https://user-images.githubusercontent.com/39123255/50836042-96a89500-139b-11e9-973c-3b305ae41087.png" width=750></p>

### 4. 안드로이드 컴파일

이제 Calorie Defender의 잠금해제 및 특정 위치 지정과 삭제 그리고 Mobius 서버에 저장된 데이터를 활용하여 데이터를 시각화하기 위한 어플리케이션을 

Calorie Defender.apk를 그대로 사용할 수 있으나 이 경우 AE_NAME을 Calorie_Defender로 두어야하며 다른 사용자와 동시에 사용시 데이터가 겹칠 수 있는 문제점이 있다.

### 5. 실행

이제 해당 페이지 맨 처음에 있는 동영상을 참고하여 앱을 사용할 수 있다.
