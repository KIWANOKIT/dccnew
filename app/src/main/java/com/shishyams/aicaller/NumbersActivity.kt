package com.shishyams.aicaller

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.shishyams.aicaller.data.Prefs
import com.shishyams.aicaller.databinding.ActivityNumbersBinding
import com.shishyams.aicaller.net.ApiClient
import com.shishyams.aicaller.net.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "My numbers": the contact numbers the admin assigned to THIS phone on the dashboard (Android Devices > Numbers). */
class NumbersActivity : AppCompatActivity() {

    private data class Entry(val name: String, val phone: String)

    private lateinit var b: ActivityNumbersBinding
    private lateinit var prefs: Prefs
    private lateinit var adapter: ArrayAdapter<Entry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityNumbersBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs.get(this)

        adapter = object : ArrayAdapter<Entry>(this, android.R.layout.simple_list_item_2, android.R.id.text1) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val e = getItem(position)!!
                val t1 = v.findViewById<TextView>(android.R.id.text1)
                val t2 = v.findViewById<TextView>(android.R.id.text2)
                t1.text = if (e.name.isNotEmpty()) e.name else e.phone
                t2.text = if (e.name.isNotEmpty()) e.phone else ""
                t1.setTextColor(ContextCompat.getColor(context, R.color.ink))
                t2.setTextColor(ContextCompat.getColor(context, R.color.slate))
                t2.visibility = if (e.name.isNotEmpty()) View.VISIBLE else View.GONE
                return v
            }
        }
        b.numList.adapter = adapter
        b.btnNumbersRefresh.setOnClickListener { load() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        b.btnNumbersRefresh.isEnabled = false
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient(prefs.serverUrl, prefs.deviceToken).myNumbers() }
                val arr = res.optJSONArray("numbers")
                adapter.clear()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        adapter.add(Entry(o.optString("name"), o.optString("phone")))
                    }
                }
                adapter.notifyDataSetChanged()
                val n = adapter.count
                b.numCount.text = resources.getQuantityString(R.plurals.numbers_count, n, n)
                b.numEmpty.visibility = if (n == 0) View.VISIBLE else View.GONE
            } catch (e: ApiException) {
                b.numCount.text = e.message
            } catch (e: Exception) {
                b.numCount.text = getString(R.string.status_offline_hint)
            } finally {
                b.btnNumbersRefresh.isEnabled = true
            }
        }
    }
}
