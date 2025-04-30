Personal android project I made for a friend. You can record clips and associate videos with those clips. When playback mode is entered, if audio similar to the clip is detected, it will play the associated video.

Audio recognition is done with the TarsosDSP and FastDTW libraries, and mainly consists of extracting MFCCs from live audio, and comparing them to a reference using DTW.
