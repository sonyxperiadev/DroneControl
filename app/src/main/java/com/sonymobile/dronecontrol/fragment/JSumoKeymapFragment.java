/*
 *  Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. Neither the name of the copyright holder nor the names
 *     of its contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sonymobile.dronecontrol.fragment;

import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.utils.KeymapAdapter;
import com.sonymobile.dronecontrol.utils.KeymapCommand;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class JSumoKeymapFragment extends Fragment {

    private ListView mKeymapListView;
    private List<KeymapCommand> mCommands = new ArrayList<KeymapCommand>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.keymap_fragment, container, false);
        addCommands();
        mKeymapListView = (ListView)view.findViewById(R.id.keymap_list);
        KeymapAdapter adapter = new KeymapAdapter(getActivity(), mCommands);
        mKeymapListView.setAdapter(adapter);
        return view;
    }

    public void addCommands() {
        mCommands.add(new KeymapCommand(R.string.command_move_back_forward, R.drawable.keymapping_analogic_left));
        mCommands.add(new KeymapCommand(R.string.command_turn_left_right, R.drawable.keymapping_analogic_right));
        mCommands.add(new KeymapCommand(R.string.command_take_pic, R.drawable.keymapping_square));
        mCommands.add(new KeymapCommand(R.string.command_long_jump, R.drawable.keymapping_directional_up));
        mCommands.add(new KeymapCommand(R.string.command_high_jump, R.drawable.keymapping_directional_down));
        mCommands.add(new KeymapCommand(R.string.command_spin, R.drawable.keymapping_analogic_left));
        mCommands.add(new KeymapCommand(R.string.command_spin_jump, R.drawable.keymapping_analogic_right));
        mCommands.add(new KeymapCommand(R.string.command_slow_shake, R.drawable.keymapping_triangle, R.drawable.keymapping_directional_up));
        mCommands.add(new KeymapCommand(R.string.command_spiral, R.drawable.keymapping_triangle, R.drawable.keymapping_directional_down));
        mCommands.add(new KeymapCommand(R.string.command_slalom, R.drawable.keymapping_triangle, R.drawable.keymapping_directional_left));
        mCommands.add(new KeymapCommand(R.string.command_ondulation, R.drawable.keymapping_triangle, R.drawable.keymapping_directional_right));
    }
}
