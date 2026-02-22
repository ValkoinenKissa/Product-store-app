package com.example.product_store_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.product_store_app.adapter.CartAdapter
import com.example.product_store_app.databinding.ActivitySecondBinding
import com.example.product_store_app.model.Product
import com.example.product_store_app.utils.CartManager.cartList
import com.example.product_store_app.utils.StatusBarColor
import com.google.android.material.snackbar.Snackbar

class SecondActivity : AppCompatActivity() {

    // View Binding: acceso directo a las vistas del layout sin findViewById()
    private lateinit var binding: ActivitySecondBinding

    // Adapter que conecta la lista del carrito con el RecyclerView
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permite que el contenido se dibuje detrás de la barra de estado del sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Inflar el layout usando View Binding
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Personalizar el color de la barra de estado del teléfono
        StatusBarColor.set(
            activity = this,
            color = getColor(R.color.primary),
            lightIcons = false
        )

        // Configurar la toolbar como ActionBar
        setSupportActionBar(binding.toolbarSecond)

        // Habilitar el botón de "volver atrás" (flecha ←) en la toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar el adapter del RecyclerView con la lista global del carrito.
        // cartList viene de CartManager (objeto singleton compartido entre Activities)
        adapter = CartAdapter(cartList, this)

        // Configurar el RecyclerView con un layout lineal (lista vertical)
        binding.recyclerCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerCart.adapter = adapter

        // Mostrar el precio total al abrir la pantalla
        updateTotal()
    }

    // Inflar el menú de la toolbar (botones de confirmar compra y vaciar carrito)
    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_second, menu)
        return true
    }

    // Manejar clics en los items del menú de la toolbar
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {

            // Botón "←" de la toolbar: cerrar esta Activity y volver a MainActivity
            android.R.id.home -> finish()

            // Botón de confirmar compra
            R.id.action_confirm -> {
                // Calcular el total sumando el precio de todos los productos del carrito
                val total = cartList.sumOf { it.price }
                val formattedTotal = "%.2f".format(total)

                // Mostrar un Snackbar con el mensaje de compra exitosa
                // Se usa getString() con placeholder para permitir traducciones
                Snackbar.make(
                    binding.root,
                    getString(R.string.purchase_success, formattedTotal),
                    Snackbar.LENGTH_LONG
                ).show()
            }

            // Botón de vaciar carrito
            R.id.action_clear -> {
                // Guardar una copia de la lista antes de vaciarla (necesaria para DiffUtil)
                val oldList = ArrayList(cartList)

                // Vaciar la lista global del carrito
                cartList.clear()

                // Usar DiffUtil para animar la eliminación de los items
                // en lugar de notifyDataSetChanged() que redibuja todos sin animación
                val diffResult = DiffUtil.calculateDiff(CartDiffCallback(oldList, cartList))
                diffResult.dispatchUpdatesTo(adapter)

                // Actualizar el texto del total (ahora será 0.00 €)
                updateTotal()

                // Mostrar confirmación al usuario
                Snackbar.make(
                    binding.root,
                    getString(R.string.cart_cleared),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Actualiza el TextView que muestra el precio total del carrito.
     * Usa getString() con placeholder (%1$s) para que el texto sea traducible.
     */
    private fun updateTotal() {
        val total = cartList.sumOf { it.price }
        val formattedTotal = "%.2f".format(total)

        // Usar string resource con placeholder en vez de concatenar strings
        // Esto resuelve los warnings:
        // - "Do not concatenate text displayed with setText"
        // - "String literal in setText can not be translated"
        binding.tvTotal.text = getString(R.string.total_price, formattedTotal)
    }

    /**
     * DiffUtil. Callback para comparar listas del carrito.
     *
     * NOTA: En el carrito un mismo producto puede aparecer varias veces
     * (el usuario puede añadir 3 unidades del mismo producto).
     * Por eso se compara por POSICIÓN además del ID, ya que dos items
     * con el mismo ID son unidades diferentes en el carrito.
     *
     * @param oldList La lista que el RecyclerView muestra actualmente
     * @param newList La nueva lista que queremos mostrar
     */
    class CartDiffCallback(
        private val oldList: List<Product>,
        private val newList: List<Product>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        // ¿Son el mismo item? Se compara por ID
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        // ¿Tienen el mismo contenido? Se compara todos los campos
        // Con data class, el == ya compara todos los campos automáticamente
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}