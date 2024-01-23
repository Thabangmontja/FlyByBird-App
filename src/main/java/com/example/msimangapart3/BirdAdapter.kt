package com.example.msimangapart3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.msimangapart3.Model.Bird

class BirdAdapter: RecyclerView.Adapter<BirdAdapter.BirdViewHolder>() {
    private var birds: List<Bird> = emptyList()

    fun setBirds(birds: List<Bird>) {
        this.birds = birds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirdViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.display_layout, parent, false)
        return BirdViewHolder(view)
    }

    override fun onBindViewHolder(holder: BirdViewHolder, position: Int) {
        val bird = birds[position]
        holder.bind(bird)
    }

    override fun getItemCount(): Int = birds.size

    class BirdViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        private var name = view.findViewById<TextView>(R.id.nameTextView)
        private var species = view.findViewById<TextView>(R.id.speciesTextView)
        private var image= view.findViewById<ImageView>(R.id.imageView2)

        fun bind(bird: Bird) {
            name.text = bird.name
            species.text = bird.species
            // Load image into the ImageView using a library like Picasso or Glide
            Glide.with(view)
                .load(bird.imageUri)
                .into(image)

        }
    }
}
