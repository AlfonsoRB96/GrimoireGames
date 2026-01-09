package com.trunder.grimoiregames

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// ¡ESTA ES LA CLAVE!
// @HiltAndroidApp activa el generador de códigos de Dagger.
@HiltAndroidApp
class GrimoireApp : Application()