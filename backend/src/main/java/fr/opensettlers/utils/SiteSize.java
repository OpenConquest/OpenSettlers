package fr.opensettlers.utils;

/**
 * Construction site grades, mirroring The Settlers II building sizes.
 * The terrain (slope) and the spacing to neighboring buildings determine the
 * largest grade allowed on a tile; a building can only be placed on a site of
 * its grade or larger.
 */
public enum SiteSize {
    /** Small hut site (woodcutter, barracks, wells…). */
    HUT,

    /** Medium house site (sawmill, bakery, watch tower…). */
    HOUSE,

    /** Large castle site (farm, warehouse, fortress, harbor…). */
    CASTLE,

    /** Mountain mine site (granite, coal, iron and gold mines). */
    MINE
}
