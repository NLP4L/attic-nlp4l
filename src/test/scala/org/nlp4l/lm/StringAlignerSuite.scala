package org.nlp4l.lm

import com.ibm.icu.text.Transliterator
import org.scalatest.FunSuite

class StringAlignerSuite extends FunSuite {

  val trans = Transliterator.getInstance("Katakana-Latin")
  val aligner = new StringAligner

  test("test variation of interface"){
    testAlign("インターフェース", "interface", "i,i/n,n/t,t/ā,er/f,f/ē,a/s,c/u,e")
    testAlign("インターフェイス", "interface", "i,i/n,n/t,t/ā,er/f,f/ei,a/s,c/u,e")
    testAlign("インタフェース", "interface", "i,i/n,n/t,t/a,er/f,f/ē,a/s,c/u,e")
    testAlign("インタフェイス", "interface", "i,i/n,n/t,t/a,er/f,f/ei,a/s,c/u,e")
  }

  test("other technical terms that have variations in katakana"){
    testAlign("テクノロジー", "technology", "t,t/e,e/k,c/u,h/n,n/o,o/r,l/o,o/j,g/ī,y")
    testAlign("テクノロジ", "technology", "t,t/e,e/k,c/u,h/n,n/o,o/r,l/o,o/j,g/i,y")
    testAlign("メール", "mail", "m,m/ē,ai/ru,l")
    testAlign("メイル", "mail", "m,m/e,a/i,i/ru,l")
    testAlign("ベクトル", "vector", "b,v/e,e/ku,c/t,t/o,o/ru,r")
    testAlign("ベクター", "vector", "b,v/e,e/ku,c/t,t/ā,or")
    testAlign("キーボード", "keyboard", "k,k/ī,ey/b,b/ō,oar/do,d")
    testAlign("キイボード", "keyboard", "k,k/i,e/i,y/b,b/ō,oar/do,d")
    testAlign("キィボード", "keyboard", "k,ke/yi,y/b,b/ō,oar/do,d")   // not good...
    testAlign("マシーン", "machine", "m,m/a,a/s,c/h,h/ī,i/n,ne")
    testAlign("マシン", "machine", "m,m/a,a/s,c/h,h/i,i/n,ne")
    testAlign("クエリー", "query", "k,q/u,u/e,e/r,r/ī,y")
    testAlign("クエリ", "query", "k,q/u,u/e,e/r,r/i,y")
    testAlign("ページランク", "pagerank", "p,p/ē,a/j,g/i,e/r,r/a,a/n,n/ku,k")
    testAlign("ペイジランク", "pagerank", "p,p/ei,a/j,g/i,e/r,r/a,a/n,n/ku,k")
    testAlign("バージョン", "version", "b,ver/ā,s/j,i/o,o/n,n")           // not good...
    testAlign("ヴァージョン", "version", "v,v/u,e/~,r/ā,s/j,i/o,o/n,n")
    testAlign("コンピューター", "computer", "k,c/o,o/n,m/py,p/ū,u/t,t/ā,er")
    testAlign("コンピュータ", "computer", "k,c/o,o/n,m/py,p/ū,u/t,t/a,er")
  }

  test("other technical terms that don't have any variations in katakana"){
    testAlign("パターン", "pattern", "p,p/a,a/t,tt/ā,er/n,n")
    testAlign("サポート", "support", "s,s/a,u/p,pp/ō,or/to,t")
    testAlign("システム", "system", "sh,s/i,y/su,s/t,t/e,e/mu,m")
    testAlign("モデリング", "modeling", "m,m/o,o/d,d/e,e/r,l/i,i/n,n/gu,g")
    testAlign("パナソニック", "panasonic", "p,p/a,a/n,n/a,a/s,s/o,o/n,n/i,i/kku,c")
    testAlign("プログラミング", "programming", "pu,p/r,r/o,o/gu,g/r,r/a,a/m,mm/i,i/n,n/gu,g")
    testAlign("アルゴリズム", "algorithm", "a,a/ru,l/g,g/o,o/r,r/i,i/z,t/u,h/mu,m")
    testAlign("カーネル", "kernel", "k,k/ā,er/n,n/e,e/ru,l")
    testAlign("アライメント", "alignment", "ar,a/a,l/i,ign/m,m/e,e/n,n/to,t")   // not so bad...
    testAlign("グーグル", "google", "g,g/ū,oo/gu,g/r,l/u,e")
    testAlign("マイクロソフト", "microsoft", "ma,m/i,ic/k,r/u,o/r,s/oso,o/fu,f/to,t")    // really bad!
    testAlign("スクリプト", "script", "su,s/ku,c/r,r/i,i/pu,p/to,t")
    testAlign("トランザクション", "transaction", "to,t/r,r/a,a/n,n/z,s/a,a/ku,c/s,t/h,i/o,o/n,n")
    testAlign("インフォメーション", "information", "i,i/n,n/f,f/o,or/m,m/ē,a/s,t/h,i/o,o/n,n")
    testAlign("グラフ", "graph", "gu,g/r,r/a,a/f,p/u,h")
    testAlign("オートマトン", "automaton", "ō,au/t,t/o,o/m,m/a,a/t,t/o,o/n,n")
    testAlign("コンテキスト", "context", "k,c/o,o/n,n/t,t/ekis,e/u,x/to,t")             // really bad!
    testAlign("インデックス", "index", "i,i/n,n/d,d/ekkus,e/u,x")                       // really bad!
    testAlign("コード", "code", "k,c/ō,o/d,d/o,e")
    testAlign("テクニカル", "technical", "t,t/e,e/k,c/u,h/n,n/i,i/k,c/a,a/ru,l")
    testAlign("サーブレット", "servlet", "s,s/ā,er/bu,v/r,l/e,e/tto,t")
    testAlign("クラスタリング", "clustering", "k,cl/ura,u/su,s/t,t/a,e/r,r/i,i/n,n/gu,g")   // not so good...
    testAlign("サーチ", "search", "s,se/ā,ar/c,c/hi,h")                                    // not so good...
    testAlign("パスワード", "password", "p,p/a,a/s,s/u,s/w,w/ā,or/do,d")
    testAlign("ウィキペディア", "wikipedia", "u~,w/i,i/k,k/i,i/p,p/e,e/d,d/i,i/a,a")
    testAlign("バイト", "byte", "b,b/ai,y/t,t/o,e")
    testAlign("サーキット", "circuit", "s,cir/ā,c/k,u/i,i/tto,t")                           // not so good...
    testAlign("エクセル", "excel", "e,e/ku,x/s,c/e,e/ru,l")
    testAlign("スカイプ", "skype", "su,s/k,k/ai,y/p,p/u,e")
    testAlign("ソフトウェア", "software", "s,s/o,o/fu,f/t,t/o,w/u,a/~,r/ea,e")              // not so bad...
    testAlign("アップル", "apple", "a,a/ppu,pp/r,l/u,e")
  }

  test("buildings, places etc."){
    testAlign("スクール", "school", "su,s/k,ch/ū,oo/ru,l")
    testAlign("スクエア", "square", "su,s/k,q/ue,u/a,are")
    testAlign("アメリカ", "america", "a,a/m,m/e,e/r,r/i,i/k,c/a,a")
    testAlign("ストリート", "street", "su,s/to,t/r,r/ī,ee/to,t")
    testAlign("スウェーデン", "sweden", "su,s/u~,w/ē,e/d,d/e,e/n,n")
  }

  test("titles etc."){
    testAlign("アシスタント", "assistant", "a,a/s,s/h,s/i,i/su,s/t,t/a,a/n,n/to,t")
    testAlign("プレジデント", "president", "pu,p/r,r/e,e/j,s/i,i/d,d/e,e/n,n/to,t")
    testAlign("エグゼクティブ", "executive", "egu,e/z,x/e,e/k,c/u,u/t,t/i,i/b,v/u,e")
    testAlign("ジャーナリスト", "journalist", "j,jo/ā,ur/n,n/a,a/r,l/i,i/su,s/to,t")
    testAlign("ドクター", "doctor", "d,d/o,o/ku,c/t,t/ā,or")
  }

  test("sports"){
    testAlign("ベースボール", "baseball", "b,b/ē,a/s,s/u,e/b,b/ō,a/ru,ll")
    testAlign("ボウリング", "bowling", "b,b/o,o/u,w/r,l/i,i/n,n/gu,g")
    testAlign("ボーリング", "bowling", "b,b/ō,ow/r,l/i,i/n,n/gu,g")
    testAlign("ボクシング", "boxing", "b,b/okus,o/h,x/i,i/n,n/gu,g")          // really bad!
    testAlign("ランニング", "running", "r,r/a,u/n',n/n,n/i,i/n,n/gu,g")
    testAlign("スイミング", "swimming", "s,s/u,w/i,i/m,mm/i,i/n,n/gu,g")
  }

  test("general terms"){
    testAlign("ラーニング", "learning", "r,le/ā,ar/n,n/i,i/n,n/gu,g")
    testAlign("シート", "seat", "s,s/h,e/ī,a/to,t")             // not so bad...
    testAlign("コンビネーション", "combination", "k,c/o,o/n,m/b,b/i,i/n,n/ē,a/s,t/h,i/o,o/n,n")
    testAlign("コーヒー", "coffee", "k,c/ō,o/h,ff/ī,ee")
    testAlign("クリスマス", "christmas", "k,c/u,h/r,r/i,i/s,s/u,t/m,m/a,a/su,s")        // not so bad...
    testAlign("コラボレーション", "collaboration", "k,c/o,o/r,ll/a,a/b,b/o,o/r,r/ē,a/s,t/h,i/o,o/n,n")    // not so bad...
    testAlign("ラッキー", "lucky", "r,l/a,u/k,c/k,k/ī,y")
    testAlign("ダイヤモンド", "diamond", "da,d/iy,i/a,a/m,m/o,o/n,n/do,d")
    testAlign("ダイアモンド", "diamond", "da,d/i,i/a,a/m,m/o,o/n,n/do,d")
    testAlign("パッケージ", "package", "p,p/a,a/k,c/k,k/ē,a/j,g/i,e")
    testAlign("リサイクル", "recycle", "r,r/i,e/s,c/ai,y/ku,c/r,l/u,e")
    testAlign("ロレックス", "rolex", "r,r/o,o/r,l/ekkus,e/u,x")
    testAlign("プロジェクト", "project", "pu,p/r,r/o,o/j,j/e,e/ku,c/to,t")
    testAlign("スケジュール", "schedule", "su,s/k,ch/ejū,e/r,d/u,ule")                   // really bad!
    testAlign("ステージ", "stage", "su,s/t,t/ē,a/j,g/i,e")
    testAlign("ステータス", "status", "su,s/t,t/ē,a/t,t/a,u/su,s")
    testAlign("サーロイン", "sirloin", "s,s/ā,i/r,rl/o,o/i,i/n,n")                  // not so bad...
    testAlign("ターミナル", "terminal", "t,t/ā,er/m,m/i,i/n,n/a,a/ru,l")
    testAlign("セオリー", "theory", "s,th/e,e/o,o/r,r/ī,y")
  }

  def testAlign(katakana: String, en: String, expected: String): Unit = {
    val exp = expected.split("/").map{ e =>
      val s = e.split(",")
      Pair(s(0), s(1))
    }
    val result = aligner.align(trans.transform(katakana), en)
    assert(exp.toList == result.toList)
  }

  def dump(katakana: String, en: String): Unit = {
    println(aligner.align(trans.transform(katakana), en, true).map{e => "%s,%s".format(e._1,e._2)}.mkString("/"))
  }
}
