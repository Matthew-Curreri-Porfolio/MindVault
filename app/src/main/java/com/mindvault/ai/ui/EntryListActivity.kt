package com.mindvault.ai.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.mindvault.ai.R
import com.mindvault.ai.data.repo.EntryRepository
import kotlinx.coroutines.*

class EntryListActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_list)

        val listView = findViewById<ListView>(R.id.entryList)
        val repo = EntryRepository(this)

        scope.launch {
            val entries = withContext(Dispatchers.Default) { repo.list() }
            val items = entries.map { "${it.summary} [${it.mood}] @ ${it.createdAt}" }
            listView.adapter = ArrayAdapter(this@EntryListActivity, android.R.layout.simple_list_item_1, items)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
