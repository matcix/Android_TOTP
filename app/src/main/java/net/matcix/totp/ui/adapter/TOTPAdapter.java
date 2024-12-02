package net.matcix.totp.ui.adapter;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import net.matcix.totp.R;
import net.matcix.totp.model.TOTPArray.TOTPEntry;
import java.util.List;

public class TOTPAdapter extends RecyclerView.Adapter<TOTPAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(int position, TOTPEntry entry);
    }

    private List<TOTPEntry> entries;
    private int progress = 0;
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TOTPAdapter(List<TOTPEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_totp, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TOTPEntry entry = entries.get(position);
        holder.nameText.setText(entry.getName());
        holder.codeText.setText(entry.getCurrentCode());
        holder.progressIndicator.setProgress(progress);

        holder.itemView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onItemClick(pos, entries.get(pos));
                        v.performClick();
                    }
                }
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void updateProgress(int progress) {
        this.progress = progress;
        notifyDataSetChanged();
    }

    public void updateCodes() {
        for (TOTPEntry entry : entries) {
            entry.updateCode();
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView codeText;
        CircularProgressIndicator progressIndicator;

        ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.nameText);
            codeText = view.findViewById(R.id.codeText);
            progressIndicator = view.findViewById(R.id.progressIndicator);
        }
    }
} 