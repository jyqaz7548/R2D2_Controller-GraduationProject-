# 안드로이드 앱(APK) 빌드 가이드

ArduD2 컨트롤러를 안드로이드 전용 앱(Capacitor + Bluetooth Classic)으로 빌드하는 절차입니다.

## 1. 패키지 설치

```bash
npm install
```

## 2. 안드로이드 프로젝트 생성 + 동기화 (최초 1회)

```bash
npx cap add android
npm run android:sync
```

이후 코드 수정 시에는 `npm run android:sync` 만 실행하면 됩니다.

## 3. 블루투스 권한 확인 (AndroidManifest.xml)

`android/app/src/main/AndroidManifest.xml`을 열어 `<manifest>` 태그 바로 안에
아래 권한이 있는지 확인하고, 없으면 추가하세요.

```xml
<!-- 구버전 안드로이드 (~ Android 11) -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- 안드로이드 12+ (API 31+) -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

`@e-is/capacitor-bluetooth-serial` 플러그인이 자동으로 일부를 병합해주지만,
빌드 후 권한이 빠져 있으면 위 항목을 수동으로 추가하세요.

## 4. Android Studio에서 열기 & 빌드

```bash
npm run android:open
```

Android Studio가 열리면:

1. Gradle Sync가 끝날 때까지 대기
2. 휴대폰을 USB로 연결 → 개발자 모드 + USB 디버깅 활성화
3. 상단 ▶ 버튼으로 바로 설치하거나,
   `Build > Build Bundle(s) / APK(s) > Build APK(s)` 로 APK 파일 생성
4. APK 경로: `android/app/build/outputs/apk/debug/app-debug.apk`

## 5. 사용 전 준비 (휴대폰)

1. 설정 > 블루투스에서 HC-06 페어링 (PIN: `1234` 또는 `0000`)
2. 앱 실행 후 "연결" 버튼 클릭
3. 목록에서 HC-06 선택 → 자동 연결

## 코드 수정 후 재빌드

```bash
npm run android:sync
```
