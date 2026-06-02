package center.oneapi.mobile.features.image;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ImageAssistantCatalog {
    private ImageAssistantCatalog() {
    }

    public static final class Assistant {
        public final String id;
        public final String name;
        public final String description;
        public final String prompt;

        private Assistant(String id, String name, String description, String prompt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.prompt = prompt;
        }
    }

    private static final List<Assistant> BUILT_INS = Collections.unmodifiableList(Arrays.asList(
            item("gpt-image2-anime-and-manga", "动画与漫画", "参考 12 条图例；二次元角色、分镜和赛璐璐上色", "Create an original anime and manga visual. Use clean cel-shaded line art, expressive faces, dynamic pose or panel rhythm, a controlled high-saturation palette, layered environment storytelling, and crisp silhouette design. Keep the characters original and avoid any existing IP."),
            item("gpt-image2-gaming", "游戏", "参考 10 条图例；游戏镜头、HUD 和可玩场景氛围", "Design an original in-game scene or key art with clear game-camera context, readable HUD or UI cues when relevant, playable space logic, environmental storytelling, and screenshot-grade lighting. Make it feel like a polished modern game capture rather than a generic fantasy illustration."),
            item("gpt-image2-retro-and-cyberpunk", "复古与赛博朋克", "参考 3 条图例；霓虹材质、复古电子与未来都市板式", "Create a retro-futurist cyberpunk board or poster with neon materials, chrome, CRT glow, scanline texture, modular props or characters, and a clear grid-based composition. Keep the designs original, high-contrast, and rooted in synthwave or dystopian city atmosphere."),
            item("gpt-image2-cinematic-and-animation", "电影与动画", "参考 5 条图例；电影级镜头调度和动画叙事定格", "Create a cinematic animation frame or storyboard-style still with filmic composition, strong key light, motion-rich blocking, atmosphere depth, and a clear emotional beat. Use production-animation clarity rather than noisy photorealism."),
            item("gpt-image2-character-design", "角色设计", "参考 2 条图例；角色设定表、视图拆解和服装材质说明", "Create an original character design sheet with a front or three-quarter hero pose, supporting views, costume or material callouts, expression variety, palette logic, and readable silhouette hierarchy. Make it feel like a production-ready design board."),
            item("gpt-image2-typography-and-posters", "字体设计和海报", "参考 13 条图例；强调版式层级、可读性和海报节奏", "Design a poster where typography hierarchy leads the composition. Put canvas ratio and layout first, keep all required display text in quotes, use crisp readable lettering, strong negative space, and intentional poster rhythm. Balance image and type like an editorial campaign asset."),
            item("gpt-image2-illustration", "插图", "参考 2 条图例；主题明确、叙事清晰的出版级插图", "Create a polished illustration with a clear focal subject, intentional shape language, controlled palette, atmospheric depth, and strong narrative clarity. Keep the rendering refined and the composition clean enough for publication."),
            item("gpt-image2-watercolor", "水彩画", "参考 2 条图例；保留纸张肌理、晕染和透明叠色", "Create a watercolor illustration with soft pigment blooms, paper grain, translucent layering, light edge bleed, and gentle tonal transitions. Preserve hand-painted texture and avoid digital plasticity."),
            item("gpt-image2-ink-and-chinese", "水墨与中国风", "参考 2 条图例；笔触、留白和宣纸质感的东方画面", "Create an ink-and-Chinese-style composition with brush energy, ink diffusion, rice-paper texture, restrained palette, calligraphic rhythm, and elegant empty space. Keep it original and culturally respectful rather than imitating a specific historical artwork."),
            item("gpt-image2-pixel-art", "像素艺术", "参考 2 条图例；像素栅格、有限色盘和游戏素材感", "Create clean pixel art with deliberate tile logic, readable clusters, controlled sprite-scale detail, limited palette discipline, and game-ready silhouettes. Preserve crisp edges and avoid smooth painterly rendering."),
            item("gpt-image2-isometric", "等距视角", "参考 2 条图例；正交等距结构、层高关系和地图逻辑", "Create a true isometric scene with precise grid logic, consistent 30-degree projection, readable height changes, modular props, and strategy-game clarity. Keep the composition crisp, balanced, and easy to navigate at a glance."),
            item("gpt-image2-product-and-food", "产品与食品", "参考 4 条图例；商业级产品主视觉和食物材质表现", "Create a premium commercial render for the user subject. Use structured composition, material-specific lighting, micro-texture, strong hero framing, premium art direction, and appetizing or tactile surface detail. Prefer brand-campaign polish over cheap e-commerce styling."),
            item("gpt-image2-brand-systems-and-identity", "品牌系统与标识", "参考 3 条图例；标识、色板、字体和延展物料展示", "Create a brand system showcase board with original logo or wordmark exploration, palette chips, type hierarchy, packaging or social applications, and cohesive visual rules across touchpoints. Make it feel like a professional identity presentation."),
            item("gpt-image2-photography", "摄影", "参考 4 条图例；真实拍摄语境、镜头感和自然瑕疵", "Create a realistic photograph with explicit capture context, believable lens behavior, natural imperfections, grounded props, and location-specific lighting. Aim for documentary or editorial credibility instead of overprocessed AI gloss."),
            item("gpt-image2-infographics-and-field-guides", "信息图表和实地指南", "参考 8 条图例；固定版区、注释和高可读信息板", "Create a field guide or infographic board with fixed layout regions, exact labels in quotes when provided, clean module hierarchy, clear callouts, and classroom or museum-grade readability. Keep the structure disciplined and text legible."),
            item("gpt-image2-research-paper-figures", "研究论文图表", "参考 21 条图例；顶会级论文 Figure、流程图和方法示意", "Create a landscape research figure with conference-paper grammar: panels, nodes, arrows, legends, exact labels in quotes, restrained academic colors, and publication-grade spacing. Prioritize diagram clarity over illustration flourish."),
            item("gpt-image2-official-openai-cookbook-examples", "OpenAI 官方 Cookbook 示例", "参考 4 条图例；以教程演示为导向的规范示例图", "Create a practical GPT Image 2 reference example with clean structure, explicit task framing, and reproducible visual logic. The result should look like a polished cookbook demo that teaches a capability clearly."),
            item("gpt-image2-edit-endpoint-showcase", "编辑端点展示", "参考 2 条图例；保留主体不变量的图像编辑与重风格化", "Create an edit-style transformation while preserving the user-stated invariants. Keep the original composition or identity cues stable where requested, and show only the intended visual changes."),
            item("gpt-image2-ui-ux-mockups", "UI/UX 模型", "参考 5 条图例；产品规格式界面、真实数据和精确排版", "Create a production-quality UI or app mockup with clear product context, device or canvas constraints, real information architecture, plausible data, crisp typography, and precise spacing. It should read like a product spec rendered into a usable interface."),
            item("gpt-image2-data-visualization", "数据可视化", "参考 5 条图例；图表族、编码规则和一致标尺的可视化", "Create a publication-grade data visualization with an explicit chart family, consistent scales, exact labels in quotes, clear legend logic, and strong visual encoding. Keep the layout clean, analytical, and easy to read."),
            item("gpt-image2-technical-illustration", "技术插图", "参考 5 条图例；爆炸图、结构件说明和编号标注", "Create a technical illustration or exploded view with ordered components, numbered callouts, material differentiation, blueprint-like clarity, and precise structural logic. Make every annotation feel instructional."),
            item("gpt-image2-architecture-and-interior", "建筑与室内设计", "参考 5 条图例；空间材质、镜头视角和真实光影关系", "Create an architectural or interior scene with a clear room or building type, realistic materials, a believable camera or lens feel, directional lighting, negative space, and accurate shadow behavior. Keep the scene calm, buildable, and publication-ready."),
            item("gpt-image2-scientific-and-educational", "科学与教育", "参考 7 条图例；课堂或科普场景下的学术说明板", "Create a scientific or educational board with exact subject naming, layered annotations, classroom-grade hierarchy, clean legend structure, a restrained academic palette, and strong explanatory clarity. Avoid decorative clutter."),
            item("gpt-image2-fashion-editorial", "时尚专题", "参考 7 条图例；杂志化造型、姿态和高级布光", "Create a fashion editorial image with styled wardrobe direction, magazine-grade composition, purposeful pose, premium lighting, material realism, and refined color grading. Keep it tasteful, adult, and publication-ready."),
            item("gpt-image2-fine-art-painting", "精美艺术绘画", "参考 5 条图例；强调媒介逻辑和画廊级完成度的艺术绘画", "Create a fine art painting with clear medium logic, deliberate brushwork, tonal hierarchy, compositional depth, and gallery-grade finish. Keep it original instead of imitating a known painting."),
            item("gpt-image2-more-illustration-styles", "更多插画风格", "参考 6 条图例；装饰性、叙事性和手工感更强的插画分支", "Create an original illustration that can flex into stylized, decorative, or narrative modes while keeping strong composition, a controlled palette, and readable subject hierarchy. Preserve a distinct handcrafted feel."),
            item("gpt-image2-cinematic-film-references", "电影参考资料", "参考 6 条图例；原创新片段的镜头参考与情绪定格", "Create a cinematic film reference still with strong shot design, believable production lighting, lens-aware framing, atmosphere depth, and a specific emotional beat. It should feel like a frame from an original film."),
            item("gpt-image2-beauty-and-lifestyle", "美妆与生活方式", "参考 2 条图例；柔和商业布光下的美妆和生活方式场景", "Create a beauty or lifestyle image with soft premium lighting, clean styling, aspirational mood, tactile surface detail, and polished editorial composition. Keep the subject elegant and commercially usable."),
            item("gpt-image2-events-and-experience", "活动与体验", "参考 2 条图例；场地、参与感和品牌触点并存的活动视觉", "Create an event or experience visual with venue context, crowd or participation cues when relevant, layered signage or touchpoints, and an immersive sense of moment. Balance brand clarity with lived atmosphere."),
            item("gpt-image2-tattoo-design", "纹身设计", "参考 4 条图例；可落针的线稿、明暗和 flash sheet 展示", "Create a tattoo design or flash sheet for the requested subject. Specify tattooable placement logic, clean linework, shading style, negative-space gaps, and a presentation that works as tattoo art rather than a generic illustration. Do not place it on real skin unless the user explicitly asks."),
            item("gpt-image2-screen-photography", "屏幕摄影", "参考 2 条图例；设备实拍语境下的屏幕显示与环境反射", "Create a realistic screen photography shot showing a monitor, laptop, phone, or interface in context with believable reflections, moire-free readability, device framing, and natural desk or room lighting. It should feel like a real captured screen, not a direct screenshot.")
    ));

    public static List<Assistant> builtIns() {
        return BUILT_INS;
    }

    private static Assistant item(String id, String name, String description, String prompt) {
        return new Assistant(id, name, description, prompt);
    }
}
