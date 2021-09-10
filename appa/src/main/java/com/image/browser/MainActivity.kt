package com.image.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.library.media.AlbumListActivity
import com.troop.freedcam.R
import org.wordpress.passcodelock.databinding.ActivityPictureBinding
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks

class MainActivity : AppCompatActivity(), PermissionCallbacks {

    private lateinit var bind: ActivityPictureBinding

    companion object {
        private const val REQUEST_CODE_PERMISSION = 111
        private const val REQUEST_CODE_LOGIN = 112
    }

    // 该应用所需权限
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bind = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(bind.getRoot())
        // setContentView(R.layout.activity_main)
        // 权限检查
        this.activation()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    /**
     * 权限检查
     */
    @SuppressLint("CheckResult")
    @AfterPermissionGranted(REQUEST_CODE_PERMISSION)
    fun activation() {
        if (EasyPermissions.hasPermissions(this, *this.permissions)) {
            this.toMainFragment()
            return
        }
        // 权限请求
        EasyPermissions.requestPermissions(this, "使用该应用需要以下权限", REQUEST_CODE_PERMISSION, *this.permissions)
    }

    /**
     * 权限被拒绝 如果这里请求的权限没有在清单文件中声明,这里会反复调用
     */
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        AppSettingsDialog.Builder(this)
            .setRationale("使用该应用需要权限")
            .setTitle("缺少权限")
            .build()
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 打开设置页面返回
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (EasyPermissions.hasPermissions(this, *this.permissions)) {
                this.activation()
            } else {
                finish() // 没有权限则关闭页面
            }
        }
        // 登录页面返回
        else if (requestCode == REQUEST_CODE_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                toMainFragment() // 如果已经登录
            } else {
                finish() // 如果未登录
            }
        }
    }

    /**
     * 跳转到首页
     */
    private fun toMainFragment() {
        val intent = Intent(this, AlbumListActivity::class.java)
        startActivity(intent)
        finish()
    }
}