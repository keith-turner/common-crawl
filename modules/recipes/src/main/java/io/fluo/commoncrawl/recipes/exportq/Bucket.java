package io.fluo.commoncrawl.recipes.exportq;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;

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

/**
 * This class encapsulates a buckets serialization code.
 */
class Bucket {
  private static final String DATA_CF_PREFIX = "data:";
  private static final String META_CF = "meta";
  private static final String NOTIFICATION_CF = "fluoRecipes";
  private static final String NOTIFICATION_CQ_PREFIX = "eq:";
  private static final String SEQ_CQ_PREFIX = "seq:";

  static Column newNotificationColumn(String queueId) {
    return new Column(NOTIFICATION_CF, NOTIFICATION_CQ_PREFIX + queueId);
  }

  private final TypedTransactionBase ttx;
  private final String qid;
  private final Bytes bucketRow;

  Bucket(TransactionBase tx, String qid, int bucket) {
    this.ttx = new TypeLayer(new StringEncoder()).wrap(tx);
    this.qid = qid;
    bucketRow = Bytes.of(qid + ":" + Integer.toString(bucket, 16));
  }

  Bucket(TransactionBase tx, Bytes bucketRow) {
    this.ttx = new TypeLayer(new StringEncoder()).wrap(tx);
    this.qid = null;
    // TODO encode in a more robust way... this method doe snot work when queue id has a :
    this.bucketRow = bucketRow;
  }

  public long getSequenceNumber(int counter) {
    return ttx.get().row(bucketRow).fam(META_CF)
        .qual(SEQ_CQ_PREFIX + Integer.toString(counter, 16)).toLong(0);
  }

  // this method is 10x faster than String.format("%016x",seq)
  private static byte[] encSeq(long l) {
    byte encodedSeq[] =
        new byte[] {'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0'};
    String seqString = Long.toString(l, 16);

    for (int i = 0, j = 16 - seqString.length(); i < seqString.length(); i++) {
      encodedSeq[j++] = (byte) seqString.charAt(i);
    }
    return encodedSeq;
  }

  public void add(long seq, byte[] key, byte[] value) {
    byte[] family = new byte[5 + key.length];
    byte[] prefix = DATA_CF_PREFIX.getBytes();
    System.arraycopy(prefix, 0, family, 0, prefix.length);
    System.arraycopy(key, 0, family, prefix.length, key.length);
    ttx.mutate().row(bucketRow).fam(family).qual(encSeq(seq)).set(value);
  }

  public void setSequenceNumber(int counter, long seq) {
    ttx.mutate().row(bucketRow).fam(META_CF).qual(SEQ_CQ_PREFIX + Integer.toString(counter, 16))
        .set(seq);
  }

  public void notifyExportObserver(byte[] key) {
    Preconditions.checkNotNull(qid);
    ttx.mutate().row(bucketRow).col(newNotificationColumn(qid)).weaklyNotify();
  }

  public Iterator<ExportEntry> getExportIterator() {
    ScannerConfiguration sc = new ScannerConfiguration();
    sc.setSpan(Span.prefix(bucketRow, new Column(DATA_CF_PREFIX)));
    RowIterator iter = ttx.get(sc);

    if (iter.hasNext()) {
      ColumnIterator cols = iter.next().getValue();
      return new ExportIterator(cols);
    } else {
      return Collections.<ExportEntry>emptySet().iterator();
    }
  }

  private class ExportIterator implements Iterator<ExportEntry> {

    private ColumnIterator cols;
    private Column lastCol;

    public ExportIterator(ColumnIterator cols) {
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
      ee.key = fam.subSequence(DATA_CF_PREFIX.length(), fam.length()).toArray();
      ee.seq = Long.parseLong(cv.getKey().getQualifier().toString(), 16);
      // TODO maybe leave as Bytes?
      ee.value = cv.getValue().toArray();

      lastCol = cv.getKey();

      return ee;
    }

    @Override
    public void remove() {
      ttx.mutate().row(bucketRow).col(lastCol).delete();
    }
  }
}
