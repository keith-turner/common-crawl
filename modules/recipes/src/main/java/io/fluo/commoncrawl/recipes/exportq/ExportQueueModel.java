package io.fluo.commoncrawl.recipes.exportq;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

import io.fluo.api.client.TransactionBase;
import io.fluo.api.config.ScannerConfiguration;
import io.fluo.api.data.Bytes;
import io.fluo.api.data.Column;
import io.fluo.api.data.Span;
import io.fluo.api.iterator.ColumnIterator;
import io.fluo.api.iterator.RowIterator;
import io.fluo.api.types.StringEncoder;
import io.fluo.api.types.TypeLayer;
import io.fluo.api.types.TypedTransactionBase;

class ExportQueueModel {

  private TypedTransactionBase ttx;

  ExportQueueModel(TransactionBase tx) {
    this.ttx = new TypeLayer(new StringEncoder()).wrap(tx);
  }

  private String getBucketRow(String qid, int bucket) {
    // TODO encode in a more robust way... this method doe snot work when queue id has a :
    // TODO refactor so that do not keep translating qid and bucket to row
    return String.format("%s:%x", qid, bucket);
  }

  public long getSequenceNumber(String qid, int bucket, int counter) {
    return ttx.get().row(getBucketRow(qid, bucket)).fam("meta").qual("seq:" + counter).toLong(0);
  }

  public void add(String qid, int bucket, long seq, byte[] key, byte[] value) {
    // TODO encode seq using lexicoders... need seq nums to sort properly
    // TODO constant for data:
    byte[] family = new byte[5 + key.length];
    byte[] prefix = "data:".getBytes();
    System.arraycopy(prefix, 0, family, 0, prefix.length);
    System.arraycopy(key, 0, family, prefix.length, key.length);
    ttx.mutate().row(getBucketRow(qid, bucket)).fam(family).qual(String.format("%016x", seq))
        .set(value);
  }

  public void setSequenceNumber(String qid, int bucket, int counter, long seq) {
    ttx.mutate().row(getBucketRow(qid, bucket)).fam("meta").qual("seq:" + counter).set(seq);
  }

  public void notifyExportObserver(String qid, int bucket, byte[] key) {
    // TODO constants
    ttx.mutate().row(getBucketRow(qid, bucket)).fam("fluoRecipes").qual("eq:" + qid).weaklyNotify();
  }

  public int getBucket(Bytes row, Column column) {
    return Integer.parseUnsignedInt(row.toString().split(":")[1], 16);
  }

  public Iterator<ExportEntry> getExportIterator(String qid, int bucket) {
    ScannerConfiguration sc = new ScannerConfiguration();
    String row = getBucketRow(qid, bucket);
    sc.setSpan(Span.prefix(row, new Column("data:")));
    RowIterator iter = ttx.get(sc);

    if (iter.hasNext()) {
      ColumnIterator cols = iter.next().getValue();
      return new ExportIterator(row, cols);
    } else {
      return Collections.<ExportEntry>emptySet().iterator();
    }
  }

  private class ExportIterator implements Iterator<ExportEntry> {

    private String row;
    private ColumnIterator cols;
    private Column lastCol;


    public ExportIterator(String row, ColumnIterator cols) {
      this.row = row;
      this.cols = cols;
    }

    @Override
    public boolean hasNext() {
      return cols.hasNext();
    }

    @Override
    public ExportEntry next() {
      Entry<Column, Bytes> cv = cols.next();

      ExportEntry ee = new ExportEntry();

      Bytes fam = cv.getKey().getFamily();
      ee.key = fam.subSequence("data:".length(), fam.length()).toArray();
      ee.seq = Long.parseLong(cv.getKey().getQualifier().toString(), 16);
      // TODO maybe leave as Bytes?
      ee.value = cv.getValue().toArray();

      lastCol = cv.getKey();

      return ee;
    }

    @Override
    public void remove() {
      ttx.mutate().row(row).col(lastCol).delete();
    }
  }
}
