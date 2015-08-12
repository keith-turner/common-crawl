package io.fluo.commoncrawl.data.util;

import io.fluo.api.data.Column;
import io.fluo.api.types.StringEncoder;
import io.fluo.api.types.TypeLayer;
import io.fluo.commoncrawl.core.ColumnConstants;

public class FluoConstants {

  public static final TypeLayer TYPEL = new TypeLayer(new StringEncoder());


  public static final Column INLINKCOUNT_COL = new Column(ColumnConstants.STATS, ColumnConstants.INLINKCOUNT);
  public static final Column OUTLINKCOUNT_COL = new Column(ColumnConstants.STATS, ColumnConstants.OUTLINKCOUNT);

}
