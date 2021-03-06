package music.elsystem.myconductor

import android.widget.TextView
import music.elsystem.myconductor.Common.RenderMode.*
import music.elsystem.myconductor.Common.logeicalSpaceX
import music.elsystem.myconductor.Common.logeicalSpaceY
import music.elsystem.myconductor.Common.noteNumPerBeat
import music.elsystem.myconductor.Common.surfaceHeight
import music.elsystem.myconductor.Common.surfaceWidth
import music.elsystem.myconductor.Common.tactType
import music.elsystem.myconductor.Common.Tact.*
import music.elsystem.myconductor.Common.renderMode
import music.elsystem.myconductor.Common.settingSurfaceHeight
import music.elsystem.myconductor.Common.settingSurfaceWidth
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.pow

class Util() {
    //左上に原点を置いてそこからビットマップのドットをの座標単位として図形を描き、
    //最終OpenGLに渡す段階で下記関数を使ってOpenGL座標に変換する。
    //下記関数の考え方としては、まず原点を中心に移動し、Display画面／論理座標で縮尺する。
    //co:convert origin

    fun coX(x: Int): Float {
        var width = 0
        if (renderMode == Setting.name) {
            width = settingSurfaceWidth
        } else {
            width = surfaceWidth
        }
        return (x - (logeicalSpaceX / 2f)) * (width / logeicalSpaceX.toFloat())
    }

    fun coY(y: Int): Float {
        var margin = 0f
        var height = 0
        if (renderMode == Setting.name) {
            height = settingSurfaceHeight
            margin = 0f
        } else {
            height = surfaceHeight
            margin = 0f
        }
        return ((logeicalSpaceY - 1 - y) - (logeicalSpaceY / 2f)) * (height / logeicalSpaceY.toFloat()) - margin
    }

    fun halfBeatFrame(tempo: Int): Int {
        //リフレッシュレート60Hzを前提に一旦設計
        //tempo = 60とは１分間に60拍刻むということ。
        //リフレッシュレート60Hzであれば1分間に3600回画面が書き換わる（onDrawFrameが実行される）ということ。
        //したがって半拍分の画面書き換え回数は1800/tempoとなる。
        var temporaryHalfBeat = 0f
        when (tactType) {
            Heavy.name, Light.name -> {
                //temporaryHalfBeatはケースにより上書きしていくので注意！（もっといい書き方ないか？）
                temporaryHalfBeat = 1800f / tempo.toFloat()
                //裏拍が３連符の場合、強制的に３の倍数に変換する。
                if (noteNumPerBeat == 3) {
                    temporaryHalfBeat = ((temporaryHalfBeat / 3f).toInt() * 3).toFloat()
                }
                //裏拍が４連符の場合、強制的に４の倍数に変換する。
                if (noteNumPerBeat == 4) {
                    temporaryHalfBeat = ((temporaryHalfBeat / 4f).toInt() * 4).toFloat()
                }
            }
            Swing.name -> {
                //Swingについては2：1とするため全体を３分割し、表拍（全体の2/3の長さ）をhalfBeatFrameと呼ぶ。
                //tactType=Swingの時、強制的に２の倍数にする。
                temporaryHalfBeat = ((3600f / tempo.toFloat()) / 3f).toInt() * 2f
            }
        }
        return temporaryHalfBeat.toInt()
    }

    fun oneBeatFrame(tempo: Int): Int {
        var temporaryOneBeatFrame = 0
        when (tactType) {
            Heavy.name, Light.name -> {
                temporaryOneBeatFrame = halfBeatFrame(tempo) * 2
            }
            Swing.name -> {
                temporaryOneBeatFrame = (halfBeatFrame(tempo) * 1.5f).toInt()
            }
        }
        return temporaryOneBeatFrame
    }

    fun oneBarFrame(rhythm: Int, tempo: Int): Int {
        return oneBeatFrame(tempo) * rhythm
    }

    fun tempoChanged(newTempo: Int, tempoText: TextView, tempoSignText: TextView) {
        tempoText.setText(newTempo.toString())
        when (newTempo) {
            in 20..42 -> tempoSignText.text = "Grave"
            in 43..49 -> tempoSignText.text = "Largo"
            in 50..53 -> tempoSignText.text = "Lento"
            in 54..59 -> tempoSignText.text = "Adagio"
            in 60..67 -> tempoSignText.text = "Adagietto"
            in 68..83 -> tempoSignText.text = "Andante"
            in 84..95 -> tempoSignText.text = "Moderate"
            in 96..119 -> tempoSignText.text = "Allegretto"
            in 120..152 -> tempoSignText.text = "Allegro"
        }
    }

    //コーディングしたプリミティブ型を GPU に転送するためにバッファ型に
    // 変換するためのユーティリティクラスで、
    //頂点座標や頂点インデックスを GPU に転送する際に利用する。
    //FloatArray、ShortArrayともに関数名はconvertで同じ。
    //これにより引数によりそれに応じた関数が呼ばれる。（関数の多重定義）
    fun convert(data: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(data.size * 4)
        bb.order(ByteOrder.nativeOrder())
        val floatBuffer = bb.asFloatBuffer()
        floatBuffer.put(data)
        floatBuffer.position(0)
        return floatBuffer
    }

    fun convert(data: ShortArray): ShortBuffer {
        val bb = ByteBuffer.allocateDirect(data.size * 2)
        bb.order(ByteOrder.nativeOrder())
        val shortBuffer = bb.asShortBuffer()
        shortBuffer.put(data)
        shortBuffer.position(0)
        return shortBuffer
    }

    companion object {
        //*************** 拡張関数 *****************************************************
        fun Int.mPow(multiplier: Double) = this.toDouble().pow(multiplier)
    }
}