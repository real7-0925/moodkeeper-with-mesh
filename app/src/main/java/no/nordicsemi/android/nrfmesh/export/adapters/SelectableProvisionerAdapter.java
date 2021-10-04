/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrfmesh.export.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.elevation.ElevationOverlayProvider;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import no.nordicsemi.android.mesh.MeshNetwork;
import no.nordicsemi.android.mesh.Provisioner;
import no.nordicsemi.android.mesh.utils.MeshAddress;
import no.nordicsemi.android.nrfmesh.R;
import no.nordicsemi.android.nrfmesh.databinding.CheckableRowItemBinding;
import no.nordicsemi.android.nrfmesh.viewmodels.MeshNetworkLiveData;

public class SelectableProvisionerAdapter extends RecyclerView.Adapter<SelectableProvisionerAdapter.ViewHolder> {

    private final List<Provisioner> mProvisioners = new ArrayList<>();
    private OnItemCheckedChangedListener mOnItemClickListener;

    public SelectableProvisionerAdapter(@NonNull final LifecycleOwner owner, @NonNull final MeshNetworkLiveData meshNetworkLiveData) {
        meshNetworkLiveData.observe(owner, networkData -> {
            final MeshNetwork network = meshNetworkLiveData.getMeshNetwork();
            final List<Provisioner> provisioners = network.getProvisioners();
            mProvisioners.clear();
            mProvisioners.addAll(provisioners);
            notifyDataSetChanged();
        });
    }

    public void setOnItemCheckedChangedListener(final OnItemCheckedChangedListener listener) {
        mOnItemClickListener = listener;
    }

    @NonNull
    @Override
    public SelectableProvisionerAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ViewHolder(CheckableRowItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final SelectableProvisionerAdapter.ViewHolder holder, final int position) {
        final Provisioner provisioner = mProvisioners.get(position);
        holder.provisionerName.setText(provisioner.getProvisionerName());
        final Context context = holder.provisionerName.getContext();
        if (provisioner.getProvisionerAddress() == null) {
            holder.provisionerSummary.setText(context.getString(R.string.unicast_address,
                    holder.provisionerName.getContext().getString(R.string.address_unassigned)));
        } else {
            holder.provisionerSummary.setText(context.getString(R.string.unicast_address,
                    MeshAddress.formatAddress(provisioner.getProvisionerAddress(), true)));
        }
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mProvisioners.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public Provisioner getItem(final int position) {
        return mProvisioners.get(position);
    }

    @FunctionalInterface
    public interface OnItemCheckedChangedListener {
        void onProvisionerCheckedChanged(@NonNull final Provisioner provisioner, final boolean isChecked);
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        View container;
        ImageView icon;
        TextView provisionerName;
        TextView provisionerSummary;
        MaterialCheckBox materialCheckBox;

        private ViewHolder(final @NonNull CheckableRowItemBinding binding) {
            super(binding.getRoot());
            container = binding.container;
            icon = binding.icon;
            provisionerName = binding.title;
            provisionerSummary = binding.subtitle;
            materialCheckBox = binding.check;
            final ElevationOverlayProvider provider = new ElevationOverlayProvider(itemView.getContext());
            final int color = provider.compositeOverlayIfNeeded(provider.getThemeSurfaceColor(), 3.5f);
            container.setBackgroundColor(color);
            icon.setImageDrawable(ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_account_key));
            materialCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mOnItemClickListener != null)
                    mOnItemClickListener.onProvisionerCheckedChanged(mProvisioners.get(getAdapterPosition()), isChecked);
            });
        }
    }
}
