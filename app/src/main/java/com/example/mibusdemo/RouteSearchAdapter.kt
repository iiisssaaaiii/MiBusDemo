package com.example.mibusdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mibusdemo.data.dto.RutaDto

class RouteSearchAdapter(
    private var routes: List<RutaDto>,
    private val onRouteClick: (RutaDto) -> Unit
) : RecyclerView.Adapter<RouteSearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvRouteName)
        val tvDesc: TextView = view.findViewById(R.id.tvRouteDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val route = routes[position]
        // Accessing properties from trazadoGeojson.features[0].properties
        val properties = route.trazadoGeojson.features.firstOrNull()?.properties
        holder.tvName.text = properties?.name ?: "Ruta sin nombre"
        holder.tvDesc.text = properties?.desc ?: "Sin descripción"
        
        holder.itemView.setOnClickListener { onRouteClick(route) }
    }

    override fun getItemCount() = routes.size

    fun updateList(newList: List<RutaDto>) {
        routes = newList
        notifyDataSetChanged()
    }
}
