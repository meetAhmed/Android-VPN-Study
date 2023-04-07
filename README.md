# Studying VPN for Android. 

<h4>Language Used:</h4>
<ol>
<li>Kotlin</li>
<li>Java</li>
</ol>
<hr/>

<h4>Code is prepared by taking help from these repositories:</h4>
<ol>
<li><a href="https://github.com/DrBrad/Android-VPN-to-Socket">DrBrad/Android-VPN-to-Socket</a></li>
<li><a href="https://github.com/Attect/Android-VpnService-Demo">Attect/Android-VpnService-Demo</a></li></ol>
<hr/>
<h5>Comments/Explanation added about working of VPN.</h5>
<hr/>

<h4>Packets retrieved from Application:</h4>
<h4>UDP (hex)</h4>
<p>45 00 00 94 0D 5F 40 00 40 11 DE 71 8E FA B5 84 0A 08 00 02 01 BB B5 63 00 80 00 00 40 CD 4D A4 2C BB 74 69 CD A5 7C ED 9C 03 2A ED 73 21 DF A3 4D FD 9D C9 31 B7 C3 48 1E 34 67 7F 7F 80 B9 F1 51 84 E4 C3 29 F8 58 27 62 D0 82 F7 C9 68 3D 36 F2 C5 79 E5 76 12 60 8C 45 83 1D 9B 50 9A 72 D8 62 BF B3 3F D4 F6 C4 66 95 D7 7F 48 85 8C 0F 5C 7D B4 41 6C 27 56 BD 90 87 A1 2D 26 DA A9 7B A5 66 C4 E9 01 FE A4 4C 4C 5D 84 FD 05 07 E9 5F D7 9C FE 49 E4</p>
<hr/>
<h4>TCP (hex)</h4>
<p>45 00 01 77 00 11 40 00 40 06 AE 36 9D F0 E3 3F 0A 08 00 02 01 BB 97 BA 00 00 0D F8 01 06 96 51 50 10 FF FF AE 5E 00 00 17 03 03 00 C1 D1 ED CA 9B C6 4A F4 DB 26 2C FC 61 49 9C 2B 99 CB 9E C7 97 E9 89 BF 1E 9D 24 E9 B2 AD 24 40 4B 5F 85 F9 5C DF 84 88 CF 0E F6 2F A8 50 86 B2 6B 97 85 FB BB B3 06 93 9E C5 7C B7 ED B1 F0 60 23 78 59 8A B9 4D 99 E8 CA 6A C8 D9 39 D1 2C B4 EE EB D1 89 43 0C 83 71 55 73 91 B4 E8 92 B3 C8 92 3E F0 83 19 3B 73 4B 91 05 EC 5C 48 17 8B D8 3D 8A 7A FE 91 7C 6E AC 3F C3 76 63 1B 27 52 EA C1 75 D0 D8 B8 F5 88 FD B3 08 F9 59 4A 98 A7 9A 42 BD 29 70 BC EF E6 8C 15 6B CD 0E 1C 76 C9 AE BF 1C 7E D5 A2 3F 64 4B 25 0A 72 16 56 BE D8 58 4A 4A 05 67 1A F1 4C 0A BE 3D 77 17 45 93 C5 33 12 B4 17 03 03 00 84 45 06 3D 63 E0 C0 4D 48 DF 47 70 58 11 3D A9 BA 31 4C 8C AA 3A DA 04 DC 21 09 38 82 9F FA 89 26 8E 30 C3 CB FA 56 CB 7F D8 F0 D1 8C 1E D0 86 AC 87 FD 48 35 B3 37 7C DB EA AA A3 4E 7D 02 B1 F4 01 0C 26 CA F4 9B 67 93 F5 AB FC 11 45 C0 A4 18 67 4B EA 7F 14 F7 5F CA 69 19 99 35 27 AE B7 68 90 83 1F E9 0F 17 95 92 D5 03 80 88 84 38 85 25 45 8E 6C F5 8A F8 2F D7 6C 4B A0 7F EB 09 8B BB A6 60 10 6B</p>
<hr/>
<h4><a href="https://hpd.gasmi.net/">Use this site to decode packet from hex.</a></h4>
<p>Note: Android Studio Logcat has a limit of maximum 4096 bytes (4KB) or (about 40 bytes) for each log. So if packet is more than this size, then whole packet may not be logged in Logcat. And upon decoding such packet "IPv4 total length exceeds packet length" error may received.</p>
<p>Note: It is observed that, Logs affect performance of VPN.</p>
<hr/>

<h4>Useful links:</h4>
<ol>
<li><a href="https://www.geeksforgeeks.org/introduction-and-ipv4-datagram-header/">IPv4 Header</a></li>
<li><a href="https://networklessons.com/cisco/ccie-routing-switching-written/tcp-header">TCP Header</a></li>
<li><a href="https://www.geeksforgeeks.org/user-datagram-protocol-udp/">UDP Header</a></li>
<li><a href="https://www.baeldung.com/kotlin/bitwise-operators">Bitwise Operations in Kotlin</a></li>
<li><a href="https://developer.android.com/reference/android/net/VpnService.Builder">VpnService</a></li>
<li><a href="https://developer.android.com/reference/java/nio/channels/Selector">Selectors</a></li>
<li><a href="https://www.baeldung.com/java-nio-selector">Introduction to the Java NIO Selector</a></li>
</ol>
<hr/>


