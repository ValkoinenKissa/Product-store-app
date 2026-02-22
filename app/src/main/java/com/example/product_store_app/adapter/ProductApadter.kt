package com.example.product_store_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.product_store_app.databinding.ItemProductBinding
import com.example.product_store_app.model.Product

class ProductAdapter(
    var lista: ArrayList<Product>,
    var contexto: Context,
    var onAddToCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.MyHolder>() {

    class MyHolder(var binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(contexto), parent, false
        )
        return MyHolder(binding)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = lista[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvPrice.text = "%.2f €".format(item.price)
        Glide.with(contexto)
            .load(item.thumbnail)
            .into(holder.binding.imgProduct)
        holder.binding.btnAddToCart.setOnClickListener {
            onAddToCart(item)
        }
    }

    override fun getItemCount() = lista.size
}