#ifndef COM_STMOBILE_SOUND_PLAY_H
#define COM_STMOBILE_SOUND_PLAY_H

#ifdef __cplusplus
extern "C" {
#endif

void soundLoad(void* sound, const char* sound_name, int length);
void soundPlay(const char* sound_name, int loop);
void soundStop(const char* sound_name);

#ifdef __cplusplus
}
#endif

#endif
