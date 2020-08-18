/*
 * Copyright (C) 2020 The exTHmUI Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exthmui.game.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import org.exthmui.game.misc.Constants;
import org.exthmui.game.services.GamingService;

public class GamingBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (Constants.Broadcasts.SYS_BROADCAST_GAMING_MODE_ON.equals(intent.getAction())) {
                Intent serviceIntent = new Intent(context, GamingService.class);
                serviceIntent.putExtras(intent);
                context.startForegroundServiceAsUser(serviceIntent, UserHandle.CURRENT);
            }
        }
    }
}
