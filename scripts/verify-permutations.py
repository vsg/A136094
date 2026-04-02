from itertools import permutations

def test(sequence):
  for p in permutations(set(sequence)):
    s = ''.join(p)
    k = -1
    for c in s:
      k = sequence.find(c, k+1)
      if k < 0:
        print(s)
        return False
  return True

#s = "1234123124123"
#s = "123451234123512341235"
#s = "123451672341526371425361274351263471253"
#s = "1231456721435612734156327143562137425361"
#s = "1234561234516234152361423516"
#s = "1234156782315426738152643718265341278635124376812534"
#s = "1234512341532145321"
#s = "1234516782341567234815263471825346127385412367581243"
s = "1234156782315426738152643718265341278635124376812453"

print(f"s = {s}, length = {len(s)}, digits = {len(set(s))}, all_permutations = {test(s)}")
