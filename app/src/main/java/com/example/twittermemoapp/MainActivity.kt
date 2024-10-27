package com.example.twittermemoapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.html.HtmlPlugin
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val memos = mutableListOf<Memo>()
    private lateinit var adapter: MemoAdapter
    private lateinit var markwon: Markwon
    private lateinit var editor: MarkwonEditor

    // ファイル操作用のリクエストコード
    private val CREATE_FILE = 1
    private val OPEN_FILE = 2
    private val AUTO_SAVE_FILE = "auto_save.md"  // 追加

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 自動保存データの読み込み
        loadAutoSave()

        // Markwonの初期化
        markwon = Markwon.builder(this)
            .usePlugin(HtmlPlugin.create())
            .build()

        editor = MarkwonEditor.create(markwon)

        // RecyclerViewの設定
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = MemoAdapter(
            memos,
            markwon,
            onMemoClick = { memo -> showMemoDialog(memo) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // スワイプ削除の設定
        setupSwipeToDelete(recyclerView)

        // メニューの設定
        setupMenu()

        // FABのクリックリスナー
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            showMemoDialog()
        }
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                deleteMemo(position)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun deleteMemo(position: Int) {
        if (position >= 0 && position < memos.size) {
            memos.removeAt(position)
            adapter.notifyItemRemoved(position)
            Toast.makeText(this, "メモを削除しました", Toast.LENGTH_SHORT).show()
            autoSave()
        }
    }

    private fun showMemoDialog(memo: Memo? = null) {
        val dialog = MemoDialogFragment.newInstance(editor, markwon, memo)
        dialog.show(supportFragmentManager, "memo_dialog")
    }

    fun addMemo(content: String) {
        memos.add(0, Memo(content = content))
        adapter.notifyItemInserted(0)
        autoSave()
    }

    fun updateMemo(memo: Memo, newContent: String) {
        val index = memos.indexOfFirst { it.id == memo.id }
        if (index != -1) {
            memos[index] = Memo(id = memo.id, content = newContent)
            adapter.notifyItemChanged(index)
            autoSave()
        }
    }

    private fun setupMenu() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // タイトルを設定
        supportActionBar?.apply {
            title = "メモ"
            setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // メニューをインフレート
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                createFile()
                true
            }
            R.id.action_open -> {
                openFile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/markdown"
            putExtra(Intent.EXTRA_TITLE, "memo.md")
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/markdown"
        }
        startActivityForResult(intent, OPEN_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }

        data?.data?.also { uri ->
            when (requestCode) {
                CREATE_FILE -> writeFile(uri)
                OPEN_FILE -> readFile(uri)
            }
        }
    }

    private fun writeFile(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val content = StringBuilder()
                memos.forEach { memo ->
                    content.append("${memo.content}\n")
                    content.append("- ${memo.timestamp}\n\n")
                }
                outputStream.write(content.toString().toByteArray())
            }
            Toast.makeText(this, "ファイルを保存しました", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "保存に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                // 既存のメモをクリア
                memos.clear()

                // マークダウンファイルを解析してメモに変換
                val sections = content.split("\n\n")
                sections.forEach { section ->
                    if (section.isNotEmpty()) {
                        // タイムスタンプが含まれている場合は抽出
                        val lines = section.trim().lines()
                        if (lines.isNotEmpty()) {
                            val content = lines.dropLast(1).joinToString("\n")
                            val timestamp = lines.lastOrNull()?.removePrefix("- ") ?: ""
                            memos.add(Memo(content = content, timestamp = timestamp))
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }
            Toast.makeText(this, "ファイルを読み込みました", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "読み込みに失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 自動保存関連のメソッド
    private fun autoSave() {
        try {
            val file = File(filesDir, AUTO_SAVE_FILE)
            file.writeText(buildString {
                memos.forEach { memo ->
                    append("${memo.content}\n")
                    append("- ${memo.timestamp}\n\n")
                }
            })
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadAutoSave() {
        try {
            val file = File(filesDir, AUTO_SAVE_FILE)
            if (file.exists()) {
                val content = file.readText()
                val sections = content.split("\n\n")
                memos.clear()
                sections.forEach { section ->
                    if (section.isNotEmpty()) {
                        val lines = section.trim().lines()
                        if (lines.isNotEmpty()) {
                            val content = lines.dropLast(1).joinToString("\n")
                            val timestamp = lines.lastOrNull()?.removePrefix("- ") ?: ""
                            memos.add(Memo(content = content, timestamp = timestamp))
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // アプリがバックグラウンドに行く時に自動保存
    override fun onPause() {
        super.onPause()
        autoSave()
    }
}