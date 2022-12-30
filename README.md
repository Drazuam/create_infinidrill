# ![Thumbnail](https://github.com/Drazuam/create_infinidrill/blob/cc307d30aecc5a7e361c7c746bba654fb1b34421/src/main/resources/META-INF/637936068355887293.png) Create InfiniDrill
Simple mod that adds a lightweight mixin to emulate the behavior of the Create hose pulley using the Create drill.  Recommended for use in combination with a [large ore veins generation mod](https://www.curseforge.com/minecraft/mc-mods/large-ore-deposits).
  
![Image](https://i.imgur.com/HbmOj65.png)
  
By default, the number of ores in a 7x7 chunk area surrounding the drill must by higher than 6,000 for it to be considered infinite.  When it is infinite, the drill will "mine" the ore block but not actually break it.  The threshold for infinite ores as well as an ore blacklist are configurable via a config file.  Under the hood, scanning for ores is done per-chunk, in another thread, and is cached for small amount of time - this means that using many drills has the same impact as using a single one, and the impact on performance should be negligible.

 

Feel free to hop in our discord if you'd like support for this mod or would like to see the pack it's going into.

 

 https://discord.gg/Rm6bNss6Vc
