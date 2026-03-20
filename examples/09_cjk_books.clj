;; =============================================================================
;; Example 09 — CJK Books: Chinese, Japanese, Korean
;; =============================================================================
;;
;; Demonstrates:
;;   - Chinese (zh): Tao Te Ching excerpt with classical typography CSS
;;   - Japanese (ja): Haiku anthology with vertical writing (writing-mode)
;;   - Korean  (ko): Modern prose excerpt with Hangul typography
;;
;; Each example is a standalone make-epub! call producing a separate file.
;; The :language field drives xml:lang on the XHTML root element.

(require '[clj-epub.core :as epub])

;; =============================================================================
;; Chinese — 道德经 (Tao Te Ching)
;; =============================================================================

(epub/make-epub!
 {:metadata {:title           "道德经"
             :language        "zh"
             :creators        [{:name    "老子"
                                :role    "aut"
                                :file-as "Laozi"}]
             :publisher       "古典书坊"
             :date            "2024-01-01"
             :description     "老子所著，道家思想的核心典籍。"
             :subjects        ["古典文学" "道家" "哲学"]}

  :chapters [{:id      "preface"
              :title   "序"
              :href    "Text/preface.xhtml"
              :content "<h1>序</h1>
                        <p>《道德经》，又称《老子》，是道家学派的经典著作，
                        约成书于春秋战国之际，共八十一章。</p>"}

             {:id      "ch01"
              :title   "第一章"
              :href    "Text/ch01.xhtml"
              :content "<h1>第一章</h1>
                        <p class=\"original\">道可道，非常道。名可名，非常名。</p>
                        <p class=\"original\">无名天地之始；有名万物之母。</p>
                        <p class=\"annotation\">【今译】可以言说的道，就不是永恒的道；
                        可以命名的名，就不是永恒的名。</p>"}

             {:id      "ch02"
              :title   "第二章"
              :href    "Text/ch02.xhtml"
              :content "<h1>第二章</h1>
                        <p class=\"original\">天下皆知美之为美，斯恶已。
                        皆知善之为善，斯不善已。</p>
                        <p class=\"annotation\">【今译】天下人都知道美之所以为美，
                        丑的概念就产生了；都知道善之所以为善，不善的概念就产生了。</p>"}

             {:id      "ch76"
              :title   "第七十六章"
              :href    "Text/ch76.xhtml"
              :content "<h1>第七十六章</h1>
                        <p class=\"original\">人之生也柔弱，其死也坚强。
                        草木之生也柔脆，其死也枯槁。</p>
                        <p class=\"original\">故坚强者死之徒，柔弱者生之徒。</p>
                        <p class=\"annotation\">【今译】人活着的时候身体是柔软的，
                        死后则变得僵硬。草木生长时是柔嫩的，死后则枯槁。
                        所以坚硬强壮属于死亡的一类，柔软弱小属于生命的一类。</p>"}]

  :css "body {
  font-family: 'Noto Serif CJK SC', 'Source Han Serif CN', STSong, serif;
  font-size: 1em;
  line-height: 2;
  margin: 6%;
  color: #1a1a1a;
}
h1 {
  font-size: 1.6em;
  text-align: center;
  margin: 1.5em 0 1em;
  letter-spacing: 0.3em;
}
p { margin: 0.8em 0; text-indent: 2em; }
p.original {
  font-weight: bold;
  color: #222;
  text-indent: 2em;
}
p.annotation {
  color: #555;
  font-size: 0.92em;
  border-left: 3px solid #bbb;
  padding-left: 1em;
  text-indent: 0;
  margin-left: 1em;
}"}

 "09a-chinese-taoteching.epub")

;; =============================================================================
;; Japanese — 俳句集 (Haiku Anthology)
;; =============================================================================
;;
;; Demonstrates vertical writing via CSS writing-mode.
;; Note: vertical layout requires reader support (Apple Books, Kobo support it;
;; Calibre does not render writing-mode).

(epub/make-epub!
 {:metadata {:title           "俳句選集"
             :language        "ja"
             :creators        [{:name    "松尾芭蕉 他"
                                :role    "aut"
                                :file-as "Matsuo, Basho"}]
             :publisher       "和歌書房"
             :date            "2024-03-01"
             :description     "江戸時代の俳人たちによる名句を集めた選集。"
             :subjects        ["俳句" "日本文学" "古典"]}

  :chapters [{:id      "intro"
              :title   "はじめに"
              :href    "Text/intro.xhtml"
              :content "<h1>はじめに</h1>
                        <p>本書は、松尾芭蕉、与謝蕪村、小林一茶など、
                        江戸時代を代表する俳人たちの名句を収めた選集です。</p>"}

             {:id      "basho"
              :title   "松尾芭蕉"
              :href    "Text/basho.xhtml"
              :content "<h1>松尾芭蕉</h1>
                        <div class=\"haiku\">
                          <p>古池や　蛙飛び込む　水の音</p>
                          <p class=\"romaji\">Furuike ya / kawazu tobikomu / mizu no oto</p>
                        </div>
                        <div class=\"haiku\">
                          <p>夏草や　兵どもが　夢の跡</p>
                          <p class=\"romaji\">Natsukusa ya / tsuwamono-domo ga / yume no ato</p>
                        </div>
                        <div class=\"haiku\">
                          <p>閑さや　岩にしみ入る　蝉の声</p>
                          <p class=\"romaji\">Shizukasa ya / iwa ni shimi iru / semi no koe</p>
                        </div>"}

             {:id      "buson"
              :title   "与謝蕪村"
              :href    "Text/buson.xhtml"
              :content "<h1>与謝蕪村</h1>
                        <div class=\"haiku\">
                          <p>菜の花や　月は東に　日は西に</p>
                          <p class=\"romaji\">Na no hana ya / tsuki wa higashi ni / hi wa nishi ni</p>
                        </div>
                        <div class=\"haiku\">
                          <p>春の海　ひねもすのたり　のたりかな</p>
                          <p class=\"romaji\">Haru no umi / hinemosu notari / notari kana</p>
                        </div>"}

             {:id      "issa"
              :title   "小林一茶"
              :href    "Text/issa.xhtml"
              :content "<h1>小林一茶</h1>
                        <div class=\"haiku\">
                          <p>我と来て　遊べや親の　ない雀</p>
                          <p class=\"romaji\">Ware to kite / asobe ya oya no / nai suzume</p>
                        </div>
                        <div class=\"haiku\">
                          <p>雪とけて　村いっぱいの　子どもかな</p>
                          <p class=\"romaji\">Yuki tokete / mura ippai no / kodomo kana</p>
                        </div>"}]

  :css "body {
  font-family: 'Noto Serif CJK JP', 'Hiragino Mincho ProN', YuMincho, serif;
  font-size: 1em;
  line-height: 2;
  margin: 6%;
  color: #1a1a1a;
}
h1 {
  font-size: 1.5em;
  letter-spacing: 0.2em;
  margin-bottom: 1.5em;
  border-bottom: 1px solid #999;
  padding-bottom: 0.4em;
}
div.haiku {
  margin: 2em 0;
  padding: 1em 1.5em;
  border-left: 3px solid #c8a96e;
  background: #fdf9f3;
}
div.haiku p {
  margin: 0.2em 0;
  text-indent: 0;
  font-size: 1.15em;
  letter-spacing: 0.15em;
}
p.romaji {
  font-size: 0.8em !important;
  color: #888;
  font-family: Georgia, serif;
  letter-spacing: 0 !important;
  margin-top: 0.5em !important;
}"}

 "09b-japanese-haiku.epub")

;; =============================================================================
;; Korean — 현대 산문 (Modern Prose)
;; =============================================================================

(epub/make-epub!
 {:metadata {:title           "봄날의 기억"
             :language        "ko"
             :creators        [{:name    "김도현"
                                :role    "aut"
                                :file-as "Kim, Dohyeon"}]
             :publisher       "서울 문학사"
             :date            "2024-05-10"
             :description     "봄을 배경으로 한 단편 소설 모음집."
             :subjects        ["한국 문학" "단편소설" "현대문학"]}

  :chapters [{:id      "ch01"
              :title   "첫 번째 이야기: 벚꽃 아래서"
              :href    "Text/ch01.xhtml"
              :content "<h1>벚꽃 아래서</h1>
                        <p>봄비가 내리던 날, 나는 오랜 친구를 만났다.
                        우리는 말없이 벚꽃 나무 아래 서서 꽃잎이 떨어지는 것을 바라보았다.</p>
                        <p>\"오랜만이야.\" 그가 먼저 말했다.
                        \"그래, 정말 오랜만이네.\" 나는 대답했다.</p>
                        <p>우리가 마지막으로 만난 것은 삼 년 전 봄이었다.
                        그때도 벚꽃이 피어 있었다.</p>"}

             {:id      "ch02"
              :title   "두 번째 이야기: 할머니의 텃밭"
              :href    "Text/ch02.xhtml"
              :content "<h1>할머니의 텃밭</h1>
                        <p>할머니는 매년 봄이 되면 텃밭에 씨앗을 심었다.
                        상추, 고추, 토마토— 여름 내내 우리 식탁을 채울 것들이었다.</p>
                        <p>\"땅이 살아있어야 사람도 살지.\" 할머니는 항상 그렇게 말씀하셨다.
                        흙을 만지는 할머니의 손은 언제나 따뜻해 보였다.</p>"}

             {:id      "ch03"
              :title   "세 번째 이야기: 봄의 끝에서"
              :href    "Text/ch03.xhtml"
              :content "<h1>봄의 끝에서</h1>
                        <p>오월의 마지막 날, 나는 창문을 열었다.
                        봄의 냄새가 아직 남아 있었지만 여름의 기운이 스며들기 시작했다.</p>
                        <p>계절은 언제나 그렇게 조용히 바뀐다.
                        우리가 알아채기도 전에.</p>"}]

  :css "body {
  font-family: 'Noto Serif CJK KR', 'Nanum Myeongjo', 'Malgun Gothic', serif;
  font-size: 1em;
  line-height: 1.9;
  margin: 6%;
  color: #1a1a1a;
  word-break: keep-all;
}
h1 {
  font-size: 1.5em;
  margin: 1.5em 0 1em;
  font-weight: bold;
  color: #222;
}
p {
  margin: 0;
  text-indent: 1em;
  margin-bottom: 0.5em;
}"}

 "09c-korean-prose.epub")

(println "Generated three CJK EPUB files:")
(println "  09a-chinese-daodejing.epub")
(println "  09b-japanese-haiku.epub")
(println "  09c-korean-prose.epub")
