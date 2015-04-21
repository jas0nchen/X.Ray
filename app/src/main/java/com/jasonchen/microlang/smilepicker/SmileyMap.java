package com.jasonchen.microlang.smilepicker;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * jasonchen
 * 2015/04/10
 */
public class SmileyMap {

	public static final int GENERAL_EMOTION_POSITION = 0;
	public static final int EMOJI_EMOTION_POSITION = 2;
	public static final int HUAHUA_EMOTION_POSITION = 1;

	private static SmileyMap instance = new SmileyMap();
	private Map<String, String> general = new LinkedHashMap<String, String>();
	private Map<String, String> huahua = new LinkedHashMap<String, String>();

	private SmileyMap() {

		/**
		 * general emotion
		 */
		general.put("[挖鼻屎]", "face_000.png");
		general.put("[泪]", "face_001.png");
		general.put("[雪人]", "face_002.png");
		general.put("[亲亲]", "face_003.png");
		general.put("[晕]", "face_004.png");
		general.put("[可爱]", "face_005.png");
		general.put("[花心]", "face_006.png");
		general.put("[汗]", "face_007.png");
		general.put("[衰]", "face_008.png");
		general.put("[偷笑]", "face_009.png");
		general.put("[打哈欠]", "face_010.png");
		general.put("[睡觉]", "face_011.png");
		general.put("[哼]", "face_012.png");
		general.put("[可怜]", "face_013.png");
		general.put("[右哼哼]", "face_014.png");
		general.put("[酷]", "face_015.png");
		general.put("[生病]", "face_016.png");
		general.put("[馋嘴]", "face_017.png");
		general.put("[害羞]", "face_018.png");
		general.put("[怒]", "face_019.png");
		general.put("[闭嘴]", "face_020.png");
		general.put("[钱]", "face_021.png");
		general.put("[嘻嘻]", "face_022.png");
		general.put("[左哼哼]", "face_023.png");
		general.put("[委屈]", "face_024.png");
		general.put("[鄙视]", "face_025.png");
		general.put("[吃惊]", "face_026.png");
		general.put("[吐]", "face_027.png");
		general.put("[懒得理你]", "face_028.png");
		general.put("[思考]", "face_029.png");
		general.put("[怒骂]", "face_030.png");
		general.put("[哈哈]", "face_031.png");
		general.put("[haha]", "face_032.png");
		general.put("[抓狂]", "face_033.png");
		general.put("[抱抱]", "face_034.png");
		general.put("[爱你]", "face_035.png");
		general.put("[鼓掌]", "face_036.png");
		general.put("[悲伤]", "face_037.png");
		general.put("[嘘]", "face_038.png");
		general.put("[呵呵]", "face_039.png");
		general.put("[感冒]", "face_040.png");
		general.put("[黑线]", "face_041.png");
		general.put("[愤怒]", "face_042.png");
		general.put("[失望]", "face_043.png");
		general.put("[做鬼脸]", "face_044.png");
		general.put("[给力]", "face_045.png");
		general.put("[good]", "face_046.png");
		general.put("[阴险]", "face_047.png");
		general.put("[困]", "face_048.png");
		general.put("[拜拜]", "face_049.png");
		general.put("[话筒]", "face_050.png");
		general.put("[太阳]", "face_051.png");
		general.put("[doge]", "face_052.png");
		general.put("[疑问]", "face_053.png");
		general.put("[笑cry]", "face_054.png");
		general.put("[挤眼]", "face_055.png");
		general.put("[赞]", "face_056.png");
		general.put("[心]", "face_057.png");
		general.put("[伤心]", "face_058.png");
		general.put("[囧]", "face_069.png");
		general.put("[奥特曼]", "face_060.png");
		general.put("[蜡烛]", "face_061.png");
		general.put("[蛋糕]", "face_062.png");
		general.put("[弱]", "face_063.png");
		general.put("[ok]", "face_064.png");
		general.put("[威武]", "face_065.png");
		general.put("[猪头]", "face_066.png");
		general.put("[月亮]", "face_067.png");
		general.put("[浮云]", "face_068.png");
		general.put("[咖啡]", "face_079.png");
		general.put("[爱心传递]", "face_070.png");
		general.put("[来]", "face_071.png");
		general.put("[熊猫]", "face_072.png");
		general.put("[帅]", "face_073.png");
		general.put("[不要]", "face_074.png");

		general.put("[爱你]", "face_113.png");
		general.put("[拳头]", "face_114.png");
		general.put("[小拇指]", "face_115.png");
		general.put("[ye]", "face_116.png");
		general.put("[握手]", "face_117.png");
		general.put("[神马]", "face_118.png");
		general.put("[晕]", "face_119.png");
		//0 - 74  113 - 119
		
		/**
		 * huahua emotion
		 */
		huahua.put("[笑哈哈]", "face_075.png");
		huahua.put("[好爱哦]", "face_076.png");
		huahua.put("[噢耶]", "face_077.png");
		huahua.put("[偷乐]", "face_078.png");
		huahua.put("[泪流满面]", "face_079.png");
		huahua.put("[巨汗]", "face_080.png");
		huahua.put("[抠鼻屎]", "face_081.png");
		huahua.put("[求关注]", "face_082.png");
		huahua.put("[好喜欢]", "face_083.png");
		huahua.put("[崩溃]", "face_084.png");
		huahua.put("[好囧]", "face_085.png");
		huahua.put("[震惊]", "face_086.png");
		huahua.put("[别烦我]", "face_087.png");
		huahua.put("[不好意思]", "face_088.png");
		huahua.put("[羞嗒嗒]", "face_089.png");
		huahua.put("[得意地笑]", "face_090.png");
		huahua.put("[纠结]", "face_091.png");
		huahua.put("[给劲]", "face_092.png");
		huahua.put("[悲催]", "face_093.png");
		huahua.put("[甩甩手]", "face_094.png");
		huahua.put("[好棒]", "face_095.png");
		huahua.put("[瞧瞧]", "face_096.png");
		huahua.put("[不想上班]", "face_097.png");
		huahua.put("[困死了]", "face_098.png");
		huahua.put("[许愿]", "face_099.png");
		huahua.put("[丘比特]", "face_100.png");
		huahua.put("[有鸭梨]", "face_101.png");
		huahua.put("[想一想]", "face_102.png");
		huahua.put("[转发]", "face_103.png");
		huahua.put("[互相膜拜]", "face_104.png");
		huahua.put("[雷锋]", "face_105.png");
		huahua.put("[杰克逊]", "face_106.png");
		huahua.put("[玫瑰]", "face_107.png");
		huahua.put("[hold住]", "face_108.png");
		huahua.put("[群体围观]", "face_109.png");
		huahua.put("[推荐]", "face_110.png");
		huahua.put("[赞啊]", "face_111.png");
		huahua.put("[被电]", "face_120.png");
		huahua.put("[霹雳]", "face_112.png");
		//75 - 112
		
	}

	public static SmileyMap getInstance() {
		return instance;
	}

	public Map<String, String> getGeneral() {
		return general;
	}

	public Map<String, String> getHuahua() {
		return huahua;
	}
}
