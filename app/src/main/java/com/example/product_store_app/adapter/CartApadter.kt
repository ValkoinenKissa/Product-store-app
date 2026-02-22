package com.example.product_store_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.product_store_app.databinding.ItemCartBinding
import com.example.product_store_app.model.Product

class CartAdapter(
    var lista: ArrayList<Product>,
    var contexto: Context
) : RecyclerView.Adapter<CartAdapter.MyHolder>() {

    class MyHolder(var binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(contexto), parent, false
        )
        return MyHolder(binding)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = lista[position]
        holder.binding.tvCartTitle.text = item.title
        holder.binding.tvCartPrice.text = "%.2f €".format(item.price)
        Glide.with(contexto)
            .load(item.thumbnail)
            .into(holder.binding.imgCartProduct)
    }

    override fun getItemCount() = lista.size
}