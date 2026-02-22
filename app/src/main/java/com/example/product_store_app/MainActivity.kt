package com.example.product_store_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.product_store_app.adapter.ProductAdapter
import com.example.product_store_app.databinding.ActivityMainBinding
import com.example.product_store_app.model.Product
import com.example.product_store_app.utils.StatusBarColor
import org.json.JSONArray
import com.example.product_store_app.utils.CartManager

class MainActivity : AppCompatActivity() {

    // View Binding: permite acceder a las vistas del layout sin usar findViewById()
    private lateinit var binding: ActivityMainBinding

    // Lista que muestra el RecyclerView (puede estar filtrada)
    private val productList = ArrayList<Product>()

    // Lista completa de todos los productos (sin filtrar), sirve como "copia maestra"
    private val allProducts = ArrayList<Product>()

    // Mapa que relaciona el nombre visible de la categoría con su slug (identificador de la API)
    // Ejemplo: "Beauty" -> "beauty", "Furniture" -> "furniture"
    // Se usa LinkedHashMap para mantener el orden de inserción
    private val categoryMap = LinkedHashMap<String, String>()

    // Adapter del RecyclerView que conecta los datos con las vistas
    private lateinit var adapter: ProductAdapter

    // Flags para controlar la carga asíncrona y evitar race conditions
    // (las llamadas a la API no terminan al mismo tiempo)
    private var productosListos = false
    private var categoriasListas = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permite que el contenido se dibuje detrás de la barra de estado del sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Inflar el layout usando View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Personalizar el color de la barra de estado del teléfono
        StatusBarColor.set(
            activity = this,
            color = getColor(R.color.primary),
            lightIcons = false
        )

        // Configurar la toolbar como ActionBar para poder usar menús
        setSupportActionBar(binding.toolbar)

        // Inicializar el adapter del RecyclerView
        // El lambda { product -> ... } se ejecuta cuando el usuario pulsa "añadir al carrito"
        adapter = ProductAdapter(productList, this) { product ->
            CartManager.cartList.add(product)
            Toast.makeText(this, "${product.title} añadido al carrito", Toast.LENGTH_SHORT).show()
        }

        // Configurar el RecyclerView con un layout lineal (lista vertical)
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = adapter

        // Configurar el listener del Spinner para filtrar productos por categoría
        binding.spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // IMPORTANTE: no filtrar hasta que los productos estén cargados.
                // El Spinner dispara este evento automáticamente al asignarle un adapter,
                // y en ese momento los productos podrían no estar listos todavía.
                if (!productosListos) return

                // Obtener el texto seleccionado en el Spinner (nombre de la categoría)
                val selected = parent.getItemAtPosition(position).toString()
                filterByCategory(selected)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Lanzar las dos llamadas a la API en paralelo
        loadCategories()
        loadProducts()
    }

    // Inflar el menú de la toolbar (el icono del carrito)
    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Manejar clics en los items del menú de la toolbar
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.action_cart) {
            // Navegar a la pantalla del carrito
            startActivity(Intent(this, SecondActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Filtra los productos según la categoría seleccionada y actualiza el RecyclerView
     * usando DiffUtil para animar los cambios de forma eficiente.
     *
     * @param selected El nombre de la categoría seleccionada en el Spinner
     */
    private fun filterByCategory(selected: String) {
        // Crear la nueva lista filtrada
        val newList = if (selected == "Todas") {
            // Sí selecciona "Todas", mostrar todos los productos
            ArrayList(allProducts)
        } else {
            // Obtener el slug correspondiente al nombre de la categoría
            // Ejemplo: "Beauty" -> "beauty"
            val slug = categoryMap[selected]
            // Filtrar solo los productos cuya categoría coincida con el slug
            ArrayList(allProducts.filter { it.category == slug })
        }

        // Usar DiffUtil para calcular las diferencias entre la lista actual y la nueva.
        // Esto es más eficiente que notifyDataSetChanged() porque:
        // - Solo actualiza los items que realmente cambiaron
        // - Genera animaciones automáticas (inserción, eliminación, movimiento)
        val diffResult = DiffUtil.calculateDiff(ProductDiffCallback(productList, newList))

        // Reemplazar el contenido de la lista actual con la nueva
        productList.clear()
        productList.addAll(newList)

        // Aplicar los cambios calculados al adapter (dispara las animaciones)
        diffResult.dispatchUpdatesTo(adapter)
    }

    /**
     * Carga las categorías desde la API y las muestra en el Spinner.
     *
     * La API devuelve un JSON array con objetos: [{"slug": "beauty", "name": "Beauty"}, ...]
     * Se guarda en categoryMap para poder traducir nombre visible -> slug al filtrar.
     */
    private fun loadCategories() {
        val url = "https://dummyjson.com/products/categories"

        // Volley gestiona las peticiones HTTP en un hilo secundario automáticamente
        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(Request.Method.GET, url, { response ->
            // Parsear la respuesta JSON
            val jsonArray = JSONArray(response)

            // Añadir "Todas" como primera opción del Spinner
            categoryMap["Todas"] = "Todas"

            // Recorrer todas las categorías y guardarlas en el mapa
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                // Clave = nombre visible ("Beauty"), Valor = slug para filtrar ("beauty")
                categoryMap[obj.getString("name")] = obj.getString("slug")
            }

            // Crear el adapter del Spinner con los nombres de las categorías
            val spinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,        // Layout para el item seleccionado
                categoryMap.keys.toList()                     // Lista de nombres visibles
            )
            // Layout para el desplegable del Spinner
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategories.adapter = spinnerAdapter

            categoriasListas = true
        }, {
            Toast.makeText(this, "Error cargando categorías", Toast.LENGTH_SHORT).show()
        })

        queue.add(request)
    }

    /**
     * Carga todos los productos desde la API y los muestra en el RecyclerView.
     *
     * ¿Se usa? Limit=0 para obtener TODOS los productos (por defecto la API solo devuelve 30).
     * Esto es necesario para que el filtro por categoría funcione correctamente con todas.
     */
    private fun loadProducts() {
        val url = "https://dummyjson.com/products?limit=0"
        val queue = Volley.newRequestQueue(this)

        // JsonObjectRequest porque la respuesta es un objeto JSON con clave "products"
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            val productsArray = response.getJSONArray("products")

            // Parsear cada producto del JSON y crear objetos Product
            for (i in 0 until productsArray.length()) {
                val obj = productsArray.getJSONObject(i)
                val product = Product(
                    id = obj.getInt("id"),
                    title = obj.getString("title"),
                    price = obj.getDouble("price"),
                    thumbnail = obj.getString("thumbnail"),
                    category = obj.getString("category")   // Este es el slug (ej: "beauty")
                )
                // Guardar en la lista maestra (nunca se modifica después)
                allProducts.add(product)
            }

            // Mostrar todos los productos inicialmente usando DiffUtil
            val diffResult = DiffUtil.calculateDiff(ProductDiffCallback(productList, allProducts))
            productList.clear()
            productList.addAll(allProducts)
            diffResult.dispatchUpdatesTo(adapter)

            // Marcar que los productos ya están listos para que el Spinner pueda filtrar
            productosListos = true

            // Si el usuario ya seleccionó una categoría en el Spinner antes de que
            // los productos terminaran de cargar, re-aplicar ese filtro ahora
            val selected = binding.spinnerCategories.selectedItem?.toString()
            if (selected != null && selected != "Todas") {
                filterByCategory(selected)
            }
        }, {
            Toast.makeText(this, "Error cargando productos", Toast.LENGTH_SHORT).show()
        })

        queue.add(request)
    }

    /**
     * DiffUtil. Callback que compara dos listas de productos para determinar
     * qué elementos se añadieron, eliminaron o cambiaron.
     *
     * Esto permite al RecyclerView actualizar SOLO los items necesarios
     * en lugar de redibujar toda la lista, mejorando el rendimiento y
     * habilitando animaciones automáticas.
     *
     * REUTILIZACIÓN: puedes copiar esta clase a cualquier proyecto,
     * solo cambia "Product" por tu modelo de datos y ajusta areItemsTheSame()
     * y areContentsTheSame() con los campos de tu modelo.
     *
     * @param oldList La lista que el RecyclerView muestra actualmente
     * @param newList La nueva lista que queremos mostrar
     */
    class ProductDiffCallback(
        private val oldList: List<Product>,
        private val newList: List<Product>
    ) : DiffUtil.Callback() {

        // Número de elementos en la lista antigua
        override fun getOldListSize(): Int = oldList.size

        // Número de elementos en la lista nueva
        override fun getNewListSize(): Int = newList.size

        // ¿Son el mismo item? Se compara por ID (identidad)
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        // ¿Tienen el mismo contenido? Se compara por todos los campos (igualdad)
        // Si usas data class, el == ya compara todos los campos automáticamente
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}