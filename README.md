<h1 align="center">
WG Tunnel
</h1>

<span align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Discord Chat](https://img.shields.io/discord/1108285024631001111.svg)](https://discord.gg/rbRRNh6H7V)

</span>

<span align="center">


[![Google Play](https://img.shields.io/badge/Google_Play-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.zaneschepke.wireguardautotunnel)

</span>

<span align="left">

This is an alternative Android Application for [WireGuard](https://www.wireguard.com/) with added features. Built using the [wireguard-android](https://github.com/WireGuard/wireguard-android) library and [Jetpack Compose](https://developer.android.com/jetpack/compose), this application was inspired by the official [WireGuard Android](https://github.com/WireGuard/wireguard-android) app.

</span>

<span align="center">

## Screenshots

<p float="center">
  <img label="Main" style="padding-right:25px" src="asset/main_screen.png" width="200" />
  <img label="Config" style="padding-left:25px" src="./asset/config_screen.png" width="200" />
  <img label="Settings" style="padding-left:25px" src="./asset/settings_screen.png" width="200" />
  <img label="Support" style="padding-left:25px" src="./asset/support_screen.png" width="200" />
</p>

<span align="left">

## Inspiration

The inspiration for this app came from the inconvenience of constantly having to turn VPN off and on while on different networks. With there being no free solution to this problem, this app was created to meet that need.

## Features

* Add tunnels via .conf file
* Auto connect to VPN based on Wi-Fi SSID
* Split tunneling by application
* Configurable Trusted Network list 
* Optional auto connect on mobile data
* Automatic service restart after reboot
* Service will stay running in background after app has been closed


## Building
    
```
$ git clone https://github.com/zaneschepke/wgtunnel
$ cd wgtunnel
$ ./gradlew assembleRelease
```

</span>
