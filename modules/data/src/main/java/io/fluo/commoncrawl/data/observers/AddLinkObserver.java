package io.fluo.commoncrawl.data.observers;

import io.fluo.api.client.TransactionBase;
import io.fluo.api.data.Bytes;
import io.fluo.api.data.Column;
import io.fluo.api.observer.AbstractObserver;
import io.fluo.api.observer.Observer;
import io.fluo.api.types.TypedTransactionBase;
import io.fluo.commoncrawl.core.ColumnConstants;
import io.fluo.commoncrawl.data.util.FluoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddLinkObserver extends AbstractObserver {

  private static final Logger log = LoggerFactory.getLogger(AddLinkObserver.class);

  @Override
  public void process(TransactionBase tx, Bytes row, Column col) throws Exception {

    // Create inlinks
    TypedTransactionBase ttx = FluoConstants.TYPEL.wrap(tx);

    String pageUri = row.toString().substring(2);
    String linkUri = col.getQualifier().toString();
    Bytes linkRow = Bytes.of(linkUri);

    log.info("Running AddLinkObserver on page {} link {}", pageUri, linkUri);

    Bytes anchorText = ttx.get(row, col);
    Long inlinkCount = ttx.get().row(linkRow).col(FluoConstants.INLINKCOUNT_COL).toLong(0);

    ttx.mutate().row(linkRow).fam(ColumnConstants.INLINKS).qual(pageUri).set(anchorText);
    ttx.mutate().row(linkRow).col(FluoConstants.INLINKCOUNT_COL).set(inlinkCount+1);
  }

  @Override
  public ObservedColumn getObservedColumn() {
    return new ObservedColumn(new Column(ColumnConstants.OUTLINKS, "com.mastemplate.www/"), NotificationType.STRONG);
  }
}
