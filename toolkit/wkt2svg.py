from shapely.geometry import MultiLineString
import shapely.wkt


if __name__ == '__main__':
    shape_list = []
    with open('../data/roads.wkt', 'r') as f:
        for line in f:
            line = line.strip()
            if line.startswith('LINESTRING') or line.startswith('MULTILINESTRING'):
                tmp = line
            elif line == '':
                shape = shapely.wkt.loads(tmp)
                if tmp.startswith('MULTI'):
                    shape_list.extend(list(shape.geoms))
                else:
                    shape_list.append(shape)
            elif line[0].isdigit() or line[0] == '(':
                tmp += line
            else:
                print("Error:", line)

        if line != '':
            tmp += line
            shape = shapely.wkt.loads(tmp)
            if tmp.startswith('MULTI'):
                shape_list.extend(list(shape.geoms))
            else:
                shape_list.append(shape)

    print(len(shape_list))
    wkt = MultiLineString(shape_list)
    with open('../data/roads.svg', 'w') as f:
        f.write(wkt._repr_svg_())
