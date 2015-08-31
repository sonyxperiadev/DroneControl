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

package com.sonymobile.dronecontrol.utils;

import com.sonymobile.dronecontrol.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class KeymapAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private ArrayList<KeymapCommand> mCommandList = new ArrayList<KeymapCommand>();

    public KeymapAdapter(final Context context, final List<KeymapCommand> commands) {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
        mCommandList = (ArrayList<KeymapCommand>)commands;
    }

    @Override
    public int getCount() {
        return mCommandList.size();
    }

    @Override
    public Object getItem(int position) {
        return mCommandList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.keymap_list_item, null);
            holder = new ViewHolder();
            holder.keyIcons.add((ImageView)convertView.findViewById(R.id.iv_command_icon1));
            holder.keyIcons.add((ImageView)convertView.findViewById(R.id.iv_command_icon2));
            holder.keyIcons.add((ImageView)convertView.findViewById(R.id.iv_command_icon3));
            holder.keyIcons.add((ImageView)convertView.findViewById(R.id.iv_command_icon4));
            holder.keyIcons.add((ImageView)convertView.findViewById(R.id.iv_command_icon5));
            holder.keyIcons.add((ImageView)convertView.findViewById(R.id.iv_command_icon6));
            holder.keyIcons.add((ImageView)convertView.findViewById(R.id.iv_command_icon7));
            holder.command = (TextView)convertView.findViewById(R.id.tv_command_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        for (ImageView iv : holder.keyIcons) {
            iv.setVisibility(View.GONE);
        }
        for (int i = 0; i < holder.keyIcons.size(); i++) {
            if (i < mCommandList.get(position).getKeyIcons().size()) {
                (holder.keyIcons.get(i)).setImageDrawable(mContext.getResources().getDrawable((mCommandList.get(position).getKeyIcons()).get(i)));
                holder.keyIcons.get(i).setVisibility(View.VISIBLE);
            }
        }
        holder.command.setText(mContext.getResources().getString(mCommandList.get(position).getCommandText()));
        return convertView;
    }

    static class ViewHolder {
        public TextView command;
        public ArrayList<ImageView> keyIcons = new ArrayList<ImageView>();
    }
}
