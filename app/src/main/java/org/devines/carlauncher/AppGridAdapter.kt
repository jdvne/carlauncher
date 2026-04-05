package org.devines.carlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_APP    = 1

/** Sealed item types used to build the flat list that drives the grid. */
private sealed class GridItem {
    data class Header(val title: String) : GridItem()
    data class App(val info: AppInfo, val isFavorite: Boolean) : GridItem()
}

class AppGridAdapter(
    private val allApps: List<AppInfo>,
    favorites: Set<String>,
    private val recentPackages: List<String>,       // ordered most-recent-first
    private val onAppClick: (AppInfo) -> Unit,
    private val onFavoriteToggled: (favorites: Set<String>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Live favorites set — mutated on long-press, persisted via callback
    val currentFavorites: MutableSet<String> = favorites.toMutableSet()

    private var items: List<GridItem> = buildItems()

    // ── ViewHolders ───────────────────────────────────────────────────

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.sectionLabel)
    }

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView  = view.findViewById(R.id.appIcon)
        val label: TextView  = view.findViewById(R.id.appLabel)
        val star: ImageView  = view.findViewById(R.id.favoriteStar)
    }

    // ── Adapter overrides ─────────────────────────────────────────────

    override fun getItemViewType(position: Int) = when (items[position]) {
        is GridItem.Header -> VIEW_TYPE_HEADER
        is GridItem.App    -> VIEW_TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_section_header, parent, false)
            )
            else -> AppViewHolder(
                inflater.inflate(R.layout.item_app, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GridItem.Header -> (holder as HeaderViewHolder).label.text = item.title
            is GridItem.App    -> bindApp(holder as AppViewHolder, item)
        }
    }

    override fun getItemCount() = items.size

    // ── Span size helper (wired in AppDrawerActivity) ─────────────────

    fun spanSizeAt(position: Int, totalSpans: Int): Int =
        if (items[position] is GridItem.Header) totalSpans else 1

    // ── Private helpers ───────────────────────────────────────────────

    private fun bindApp(holder: AppViewHolder, item: GridItem.App) {
        holder.icon.setImageDrawable(item.info.icon)
        holder.label.text = item.info.label
        holder.star.visibility = if (item.isFavorite) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onAppClick(item.info) }
        holder.itemView.setOnLongClickListener {
            val pkg = item.info.packageName
            if (pkg in currentFavorites) currentFavorites.remove(pkg)
            else currentFavorites.add(pkg)
            onFavoriteToggled(currentFavorites)
            rebuildAndNotify()
            true
        }
    }

    private fun rebuildAndNotify() {
        items = buildItems()
        notifyDataSetChanged()
    }

    private fun buildItems(): List<GridItem> {
        val favPkgs    = currentFavorites
        val appsByPkg  = allApps.associateBy { it.packageName }

        val favApps    = allApps
            .filter { it.packageName in favPkgs }
            .sortedBy { it.label.lowercase() }

        // Recent: preserve launch order, skip anything already in Favorites
        val recentApps = recentPackages
            .filter { it !in favPkgs }
            .mapNotNull { appsByPkg[it] }   // drops uninstalled packages silently

        val recentPkgs = recentApps.map { it.packageName }.toSet()

        val otherApps  = allApps
            .filter { it.packageName !in favPkgs && it.packageName !in recentPkgs }
            .sortedBy { it.label.lowercase() }

        return buildList {
            if (favApps.isNotEmpty()) {
                add(GridItem.Header("Favorites"))
                favApps.forEach { add(GridItem.App(it, isFavorite = true)) }
            }
            if (recentApps.isNotEmpty()) {
                add(GridItem.Header("Recent"))
                recentApps.forEach { add(GridItem.App(it, isFavorite = false)) }
            }
            if (favApps.isNotEmpty() || recentApps.isNotEmpty()) {
                add(GridItem.Header("All Apps"))
            }
            otherApps.forEach { add(GridItem.App(it, isFavorite = false)) }
        }
    }
}
